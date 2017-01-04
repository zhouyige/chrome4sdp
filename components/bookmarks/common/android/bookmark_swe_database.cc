/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "components/bookmarks/common/android/bookmark_swe_database.h"

#include "base/path_service.h"
#include "base/time/time.h"
#include "base/files/file_path.h"
#include "base/files/file_util.h"
#include "base/strings/utf_string_conversions.h"
#include "sql/statement.h"
#include "components/url_formatter/url_fixer.h"
#include <queue>

using namespace bookmarks;
using namespace std;

namespace swe {

bool MigrateBookmarks(BookmarkLoadDetails* details) {
  if (nullptr == details)
    return false;

  BookmarkNode* root = details->mobile_folder_node();
  if (nullptr == root)
    return false;

  BookmarkDatabase swe_bookmark_db;
  bool success = swe_bookmark_db.Open();
  if (!success) {
    return false;
  }

  bool exists = false;
  success = swe_bookmark_db.TableExists(exists);
  if (!(success && exists)) {
    return false;
  }

  int64_t max_id = details->max_id();
  int64_t old_max_id = max_id;
  success = swe_bookmark_db.Load(max_id);
  if (!success) {
    return false;
  }

  success = swe_bookmark_db.MoveChildrenTo(root);
  if (!success) {
    return false;
  }

  success = swe_bookmark_db.DeleteTable();
  if (!success) {
    return false;
  }

  swe_bookmark_db.Close();

  if (max_id > old_max_id) {
    details->set_max_id(max_id);
    details->set_ids_reassigned(true);
  }

  return true;
}

namespace {
const base::FilePath::CharType kDatabaseRelativePath[] =
  FILE_PATH_LITERAL("../databases/browser2.db");
const char kTableName[] = "bookmarks";
}

BookmarkDatabase::BookmarkDatabase()
  : last_error_(0) {
  db_.set_error_callback(base::Bind(&swe::BookmarkDatabase::ErrorCallback,
    base::Unretained(this)));
}

BookmarkDatabase::~BookmarkDatabase() {
  Close();
}

bool BookmarkDatabase::Open() {
  base::FilePath app_path;
  bool r = PathService::Get(base::DIR_ANDROID_APP_DATA, &app_path);
  if (!r)
    return false;

  base::FilePath swe_db_path = app_path.Append(kDatabaseRelativePath);

  r = db_.Open(swe_db_path);
  if (!r)
    return false;

  return true;
}

bool BookmarkDatabase::Load(int64_t& max_id) {
  if (nullptr != root_)
    return false;   // cannot load again

  bool r = db_.is_open();
  if (!r)
    return false;

  sql::Statement sql_stmt(db_.GetUniqueStatement(
    "SELECT _id, url, folder, title, created, modified "
    "FROM BOOKMARKS "
    "WHERE parent = ? OR (parent IS NULL AND ? IS NULL)"));

  enum {
    COL_INDEX_ID,
    COL_INDEX_URL,
    COL_INDEX_FOLDER,
    COL_INDEX_TITLE,
    COL_INDEX_CREATED,
    COL_INDEX_MODIFIED
  };

  int64_t start_max_id = max_id;
  bool is_root = true;
  unique_ptr<BookmarkNode> root(new BookmarkNode(GURL()));

  // Construct the bookmark tree breadth wise
  std::queue<BookmarkNode*> q;
  q.push(root.get());

  int level = 0;
  int pops_to_level_increase = 1;   // for root node
  while (!q.empty()) {
    BookmarkNode* parent_node = q.front();
    q.pop();

    pops_to_level_increase--;

    if (level > kMaxLevels) {
      break;
    }

    if (is_root) {
      sql_stmt.BindNull(0);
      sql_stmt.BindNull(1);
      is_root = false;
    } else {
      int64_t id = parent_node->id() - start_max_id;
      sql_stmt.BindInt64(0, id);
      sql_stmt.BindInt64(1, id);
    }

    while (sql_stmt.Step()) {
      base::string16 child_title;
      if (sql::COLUMN_TYPE_NULL != sql_stmt.ColumnType(COL_INDEX_TITLE))
        child_title = sql_stmt.ColumnString16(COL_INDEX_TITLE);
      else
        continue;

      bool child_folder =
        (0 == sql_stmt.ColumnInt(COL_INDEX_FOLDER)) ? false : true;

      GURL child_url;
      if (!child_folder &&
        sql::COLUMN_TYPE_NULL != sql_stmt.ColumnType(COL_INDEX_URL)) {
        std::string url = sql_stmt.ColumnString(COL_INDEX_URL);
        child_url = url_formatter::FixupURL(url, std::string());
        if (!child_url.is_valid())
          continue;
      }

      unique_ptr<BookmarkNode> child_node(new BookmarkNode(child_url));
      if (nullptr == child_node) {
        // Out of memory. Since we are returning from here, unique_ptr will
        // delete root. BookmarkNode derives from TreeNode and TreeNode will
        // delete all its children when root is deleted.
        return false;
      }

      int64_t child_id = sql_stmt.ColumnInt64(COL_INDEX_ID) + start_max_id;

      child_node->set_id(child_id);
      child_node->set_type(1 == child_folder ? BookmarkNode::FOLDER :
        BookmarkNode::URL);
      child_node->SetTitle(child_title);

      // Ignore SWE bookmark time and use Now() instead since times make sense for
      // sync only and it is better to use current date for that purpose.
      child_node->set_date_added(base::Time::Now());

      if (child_folder)
          child_node->set_date_folder_modified(base::Time::Now());

      q.push(child_node.get());
      parent_node->Add(std::move(child_node), parent_node->child_count());
      max_id = std::max(child_id, max_id);
    }

    sql_stmt.Reset(true);

    if (pops_to_level_increase <= 0) {
      level++;
      pops_to_level_increase = q.size();
    }
  }

  root_.swap(root);
  return true;
}

bool BookmarkDatabase::TableExists(bool &exists) {
  bool r = db_.is_open();
  if (!r)
    return false;

  exists = db_.DoesTableExist(kTableName);

  return true;
}

bool BookmarkDatabase::DeleteTable() {
  bool r = db_.is_open();
  if (!r)
    return false;

  std::string sql("DROP TABLE IF EXISTS ");
  sql += kTableName;

  return db_.Execute(sql.c_str());
}

void BookmarkDatabase::Close() {
  db_.Close();
}

bool BookmarkDatabase::MoveChildrenTo(BookmarkNode* to_root) {
  if (nullptr == to_root)
    return false;

  if (nullptr == root_ || root_->empty())
    return true;    // nothing to merge

  BookmarkNode* from_root = (BookmarkNode*)root_->GetChild(0);
  if (nullptr == from_root)
    return false;

  while (!from_root->empty()) {
    BookmarkNode * child = from_root->GetChild(0);
    to_root->Add(std::move(from_root->Remove(child)), to_root->child_count());
  }
  return true;
}

void BookmarkDatabase::ErrorCallback(int error, sql::Statement* stmt) {
  last_error_ = error;
}

}

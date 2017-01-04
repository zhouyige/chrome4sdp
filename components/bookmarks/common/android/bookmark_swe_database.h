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


#ifndef COMPONENTS_BOOKMARKS_BROWSER_BOOKMARK_SWE_DATABASE_H_
#define COMPONENTS_BOOKMARKS_BROWSER_BOOKMARK_SWE_DATABASE_H_

#include "components/bookmarks/browser/bookmark_storage.h"
#include "sql/connection.h"

namespace swe {

bool MigrateBookmarks(bookmarks::BookmarkLoadDetails* details);

class BookmarkDatabase {
  public:
    BookmarkDatabase();
    ~BookmarkDatabase();

    bool Open();
    bool Load(std::int64_t& max_id);
    bool TableExists(bool& exists);
    bool DeleteTable();
    void Close();
    bool MoveChildrenTo(bookmarks::BookmarkNode* to_root);

  private:
    DISALLOW_COPY_AND_ASSIGN(BookmarkDatabase);

    void ErrorCallback(int error, sql::Statement* stmt);

    static const int kMaxLevels = 100;

    sql::Connection db_;
    std::unique_ptr<bookmarks::BookmarkNode> root_;
    int last_error_;
};

}

#endif  // COMPONENTS_BOOKMARKS_BROWSER_BOOKMARK_SWE_DATABASE_H_

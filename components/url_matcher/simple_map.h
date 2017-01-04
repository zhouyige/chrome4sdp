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

#ifndef SIMPLE_MAP_H_
#define SIMPLE_MAP_H_

#include <algorithm>
#include <vector>

// Replacement for std::map.
// Uses only 1/8th of memory compared to std::map
namespace url_matcher {

template <typename key_type, typename value_type>
class simple_map {
public:
  typedef std::vector<key_type> key_list;
  typedef std::vector<value_type> value_list;
  typedef std::pair<key_type, value_type> entry_type;

  class const_iterator;

  class iterator {
  public:
    inline iterator() {}

    inline iterator(const typename simple_map::iterator& other)
      : key_entry_(other.key_entry_), value_entry_(other.value_entry_) {}

    inline iterator(const typename simple_map::const_iterator& other)
      : key_entry_(other.key_entry_), value_entry_(other.value_entry_) {}

    inline entry_type* operator->() {
      key_value_ = std::make_pair(*key_entry_, *value_entry_);
      return &key_value_;
    }

    inline entry_type& operator*() {
      key_value_ = std::make_pair(*key_entry_, *value_entry_);
      return key_value_;
    }

    inline iterator& operator++() {
      ++key_entry_;
      ++value_entry_;
      return *this;
    }

    inline iterator operator++(int /*unused*/) {
      return iterator(key_entry_++, value_entry_++);
    }

    inline iterator& operator--() {
      --key_entry_;
      --value_entry_;
      return *this;
    }

    inline iterator operator--(int /*unused*/) {
      return iterator(key_entry_--, value_entry_--);
    }

    inline bool operator!=(const iterator& other) {
      return (key_entry_ != other.key_entry_);
    }

    inline bool operator!=(const const_iterator& other) {
      return (key_entry_ != other.key_entry_);
    }

    inline bool operator==(const iterator& other) {
      return (key_entry_ == other.key_entry_);
    }

    inline bool operator==(const const_iterator& other) {
      return (key_entry_ == other.key_entry_);
    }

  private:
    inline explicit iterator(typename key_list::iterator key_entry, typename value_list::iterator value_entry)
      : key_entry_(key_entry)
      , value_entry_(value_entry) {}

    typename key_list::iterator key_entry_;
    typename value_list::iterator value_entry_;
    entry_type key_value_;

    friend class const_iterator;
    friend class simple_map;
  };

  class const_iterator {
  public:
    inline const_iterator() {}

    inline const_iterator(const typename simple_map::const_iterator& other)
      : key_entry_(other.key_entry_), value_entry_(other.value_entry_) {}

    inline const_iterator(const typename simple_map::iterator& other)
      : key_entry_(other.key_entry_), value_entry_(other.value_entry_) {}

    inline const entry_type* operator->() const {
      key_value_ = std::make_pair(*key_entry_, *value_entry_);
      return &key_value_;
    }

    inline const entry_type& operator*() const {
      key_value_ = std::make_pair(*key_entry_, *value_entry_);
      return key_value_;
    }

    inline const_iterator& operator++() {
      ++key_entry_;
      ++value_entry_;
      return *this;
    }

    inline const_iterator operator++(int /*unused*/) {
      return const_iterator(key_entry_++, value_entry_++);
    }

    inline const_iterator& operator--() {
      --key_entry_;
      --value_entry_;
      return *this;
    }

    inline const_iterator operator--(int /*unused*/) {
      return const_iterator(key_entry_--, value_entry_--);
    }

    inline bool operator!=(const const_iterator& other) {
      return (key_entry_ != other.key_entry_);
    }

    inline bool operator!=(const iterator& other) {
      return (key_entry_ != other.key_entry_);
    }

    inline bool operator==(const const_iterator& other) {
      return (key_entry_ == other.key_entry_);
    }

    inline bool operator==(const iterator& other) {
      return (key_entry_ == other.key_entry_);
    }

  private:
    inline explicit const_iterator(typename key_list::const_iterator key_entry, typename value_list::const_iterator value_entry)
      : key_entry_(key_entry)
      , value_entry_(value_entry) {}

    typename key_list::const_iterator key_entry_;
    typename value_list::const_iterator value_entry_;
    mutable entry_type key_value_;

    friend class iterator;
    friend class simple_map;
  };

  bool empty() const { return keys_.empty(); }

  size_t size() const { return keys_.size(); }

  std::pair<iterator, bool> insert(const entry_type& item) {
    auto key_pos = std::lower_bound(keys_.begin(), keys_.end(), item.first);
    if (key_pos != keys_.end() && (item.first == *key_pos)) {
      const auto value_pos = values_.begin() + std::distance(keys_.begin(), key_pos);
      return std::make_pair(iterator(key_pos, value_pos), false);
    } else {
      auto value_pos = values_.begin() + std::distance(keys_.begin(), key_pos);
      return std::make_pair(iterator(keys_.insert(key_pos, item.first), values_.insert(value_pos, item.second)), true);
    }
  }

  value_type& operator[](const key_type& key) {
    auto key_pos = std::lower_bound(keys_.begin(), keys_.end(), key);
    if (key_pos != keys_.end() && (key == *key_pos)) {
      u_int32_t index = std::distance(keys_.begin(), key_pos);
      return values_[index];
    } else {
      key_pos = keys_.insert(key_pos, key);
      auto value_pos = values_.begin() + std::distance(keys_.begin(), key_pos);
      return *(values_.insert(value_pos, value_type()));
    }
  }

  iterator find(const key_type& key) {
    auto key_pos = std::lower_bound(keys_.begin(), keys_.end(), key);
    if (key_pos != keys_.end() && (key == *key_pos)) {
        const auto value_pos = values_.begin() + std::distance(keys_.begin(), key_pos);
        return iterator(key_pos, value_pos);
      }
    return end();
  }

  const_iterator find(const key_type& key) const {
    const auto key_pos = std::lower_bound(keys_.begin(), keys_.end(), key);
    if (key_pos != keys_.end() && (key == *key_pos)) {
        const auto value_pos = values_.begin() + std::distance(keys_.begin(), key_pos);
        return const_iterator(key_pos, value_pos);
      }
    return end();
  }

  size_t count(const key_type& key) const {
    return (find(key) == end()) ? 0 : 1;
  }

  void erase(const iterator& position) {
    keys_.erase(position.key_entry_);
    values_.erase(position.value_entry_);
  }

  size_t erase(const key_type& key) {
    const auto key_pos = std::lower_bound(keys_.begin(), keys_.end(), key);
    if (key_pos != keys_.end() && (key == *key_pos)) {
      const auto value_pos = values_.begin() + std::distance(keys_.begin(), key_pos);
      keys_.erase(key_pos);
      values_.erase(value_pos);
      return 1u;
    }
    return 0u;
  }

  void clear() {
    keys_.clear();
    values_.clear();
  }

  void operator=(const simple_map& other) {
    if (&other == this)
      return;
    keys_ = other.keys_;
    values_ = other.values_;
  }

  void shrink_to_fit() {
    keys_.shrink_to_fit();
    values_.shrink_to_fit();
  }

  iterator begin() { return iterator(keys_.begin(), values_.begin()); }
  iterator end()  { return iterator(keys_.end(), values_.end()); }

  const_iterator begin() const { return const_iterator(keys_.begin(), values_.begin()); }
  const_iterator end()  const { return const_iterator(keys_.end(), values_.end()); }

private:
  key_list keys_;
  value_list values_;
};

} // namespace url_matcher

#endif // SIMPLE_MAP_H_

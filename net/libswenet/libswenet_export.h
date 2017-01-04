// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef NET_LIBSWENET_LIBSWENET_EXPORT_H_
#define NET_LIBSWENET_LIBSWENET_EXPORT_H_

#if !defined(COMPILE_CONTENT_STATICALLY)
#if defined(WIN32)

#if defined(CONTENT_IMPLEMENTATION)
#define LIBSWENET_EXPORT __declspec(dllexport)
#else
#define LIBSWENET_EXPORT __declspec(dllimport)
#endif  // defined(CONTENT_IMPLEMENTATION)

#else // defined(WIN32) 17
#define LIBSWENET_EXPORT __attribute__((visibility("default")))
#endif

#else // !defined(COMPILE_CONTENT_STATICALLY) 21
#define LIBSWENET_EXPORT __attribute__((visibility("default")))
#endif

#endif  // NET_LIBSWENET_LIBSWENET_EXPORT_H

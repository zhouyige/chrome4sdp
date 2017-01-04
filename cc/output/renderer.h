// Copyright 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef CC_OUTPUT_RENDERER_H_
#define CC_OUTPUT_RENDERER_H_

#include <memory>

#include "base/macros.h"
#include "cc/base/cc_export.h"
#include "cc/output/compositor_frame_metadata.h"
#include "cc/output/renderer_capabilities.h"
#include "cc/output/renderer_settings.h"
#include "cc/resources/returned_resource.h"
#include "ui/gfx/color_space.h"
#include "ui/gfx/geometry/rect.h"
#include "ui/gfx/sweadreno_texture_memory.h"

namespace cc {

class RenderPass;
class RenderPassId;
class ScopedResource;
class Task;

typedef std::vector<std::unique_ptr<RenderPass>> RenderPassList;

struct RendererCapabilitiesImpl {
  RendererCapabilitiesImpl();
  ~RendererCapabilitiesImpl();

  // Capabilities copied to main thread.
  ResourceFormat best_texture_format;
  bool allow_partial_texture_updates;
  int max_texture_size;
  bool using_shared_memory_resources;

  // Capabilities used on compositor thread only.
  bool using_partial_swap;
  // Whether it's valid to SwapBuffers with an empty rect. Trivially true when
  // |using_partial_swap|.
  bool allow_empty_swap;
  bool using_egl_image;
  bool using_image;
  bool using_discard_framebuffer;
  bool allow_rasterize_on_demand;
  int max_msaa_samples;
#ifdef DO_TILED_RENDERING
  bool use_tiled_rendering;
#endif
#ifdef DO_PARTIAL_SWAP
  bool using_swap_buffer_with_damage;
#endif

  RendererCapabilities MainThreadCapabilities() const;
};

class CC_EXPORT Renderer {
 public:
  virtual ~Renderer() {}

  virtual const RendererCapabilitiesImpl& Capabilities() const = 0;

  virtual void DecideRenderPassAllocationsForFrame(
      const RenderPassList& render_passes_in_draw_order) {}
  virtual bool HasAllocatedResourcesForTesting(RenderPassId id) const;

  // This passes ownership of the render passes to the renderer. It should
  // consume them, and empty the list. The parameters here may change from frame
  // to frame and should not be cached.
  // The |device_viewport_rect| and |device_clip_rect| are in non-y-flipped
  // window space.
  virtual void DrawFrame(RenderPassList* render_passes_in_draw_order,
                         float device_scale_factor,
                         const gfx::ColorSpace& device_color_space,
                         const gfx::Rect& device_viewport_rect,
                         const gfx::Rect& device_clip_rect) = 0;

  // Puts backbuffer onscreen.
  virtual void SwapBuffers(CompositorFrameMetadata metadata) = 0;
  virtual void ReclaimResources(const ReturnedResourceArray& resources) {}

  bool visible() const { return visible_; }
  void SetVisible(bool visible);

 protected:
  explicit Renderer(const RendererSettings* settings)
      : settings_(settings), visible_(false) {}

  virtual void DidChangeVisibility() {}

  const RendererSettings* settings_;
  bool visible_;

 private:
  DISALLOW_COPY_AND_ASSIGN(Renderer);
};

}  // namespace cc

#endif  // CC_OUTPUT_RENDERER_H_
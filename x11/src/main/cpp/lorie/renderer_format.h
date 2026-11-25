#pragma once

#include <stdbool.h>
#include <stdint.h>

/**
 * Determines whether a GPU-offloaded Present copy needs an R/B channel swizzle.
 *
 * Constants mirror <android/hardware_buffer.h> and buffer.h without requiring NDK headers:
 * - LORIEBUFFER_AHARDWAREBUFFER == 3
 * - AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM == 1
 * - AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM == 2
 * - AHARDWAREBUFFER_FORMAT_B8G8R8A8_UNORM == 5
 */
static inline uint8_t lorieShouldSwizzleGpuCopy(uint8_t srcType,
                                                uint8_t srcFormat,
                                                bool srcIsRgba,
                                                bool dstIsRgba) {
    // External Vulkan/DRI3 clients import AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM (1)
    // holding true (R,G,B,A) pixels, whereas Lorie itself only allocates R8G8B8X8 (2)
    // or B8G8R8A8 (5). When dst is R8G8B8X8 (dstIsRgba == true), redrawLocked() applies
    // .bgra on display, so the copy into dst must swap R and B channels.
    if (srcType == 3 /* LORIEBUFFER_AHARDWAREBUFFER */ &&
        srcFormat == 1 /* AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM */) {
        return dstIsRgba ? 1 : 0;
    }
    return (srcIsRgba != dstIsRgba) ? 1 : 0;
}

/**
 * Determines whether drawing the root/flipped texture to the Android EGL surface
 * needs the .bgra shader swizzle.
 */
static inline uint8_t lorieShouldSwizzleScreenDraw(uint8_t srcType,
                                                  uint8_t srcFormat,
                                                  bool isRgba) {
    // External Vulkan/DRI3 AHB buffers in R8G8B8A8_UNORM (1) store true RGBA order,
    // unlike Lorie's X11 R8G8B8X8 (2) root which stores X11 BGRX pixels.
    if (srcType == 3 /* LORIEBUFFER_AHARDWAREBUFFER */ &&
        srcFormat == 1 /* AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM */) {
        return 0;
    }
    return isRgba ? 1 : 0;
}

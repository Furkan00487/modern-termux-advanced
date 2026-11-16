#include <cassert>
#include <cstdio>

#include "../../main/cpp/lorie/renderer_format.h"

namespace {
constexpr uint8_t kLorieBufferFd = 2;
constexpr uint8_t kLorieBufferAhb = 3;

constexpr uint8_t kAhbFormatRgba8888 = 1;
constexpr uint8_t kAhbFormatRgbx8888 = 2;
constexpr uint8_t kAhbFormatBgra8888 = 5;
}  // namespace

int main() {
    // 1. Mali/Exynos root (R8G8B8X8 -> dstIsRgba=true) + external Chromium Vulkan AHB (R8G8B8A8=1).
    //    Chromium stores true [R,G,B,A]; root expects X11 [B,G,R] before redrawLocked .bgra display swizzle.
    assert(lorieShouldSwizzleGpuCopy(kLorieBufferAhb, kAhbFormatRgba8888, true, true) == 1);

    // 2. Adreno root (B8G8R8A8 -> dstIsRgba=false) + external Chromium Vulkan AHB (R8G8B8A8=1).
    assert(lorieShouldSwizzleGpuCopy(kLorieBufferAhb, kAhbFormatRgba8888, true, false) == 0);

    // 3. Internal Lorie AHB pixmap (R8G8B8X8=2, X11 CPU byte order) + Mali root (dstIsRgba=true).
    assert(lorieShouldSwizzleGpuCopy(kLorieBufferAhb, kAhbFormatRgbx8888, true, true) == 0);

    // 4. FD/SHM pixmap (B8G8R8A8=5 -> srcIsRgba=false) + Mali root (dstIsRgba=true).
    assert(lorieShouldSwizzleGpuCopy(kLorieBufferFd, kAhbFormatBgra8888, false, true) == 1);

    // 5. FD/SHM pixmap (B8G8R8A8=5 -> srcIsRgba=false) + Adreno root (dstIsRgba=false).
    assert(lorieShouldSwizzleGpuCopy(kLorieBufferFd, kAhbFormatBgra8888, false, false) == 0);

    std::puts("renderer_format_test: all 5 assertions passed");
    return 0;
}

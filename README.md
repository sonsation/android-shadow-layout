[![](https://jitpack.io/v/sonsation/shadow-layout.svg)](https://jitpack.io/#sonsation/shadow-layout)


Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    compile 'com.github.sonsation:shadow-layout:${version}'
}
```

# Custom View Background and Styling Methods Reference

This document serves as a reference for the available methods to update and customize the background, radius, shadows, stroke, and gradient of a custom view.

## 1. Background Customization

- **`updateBackgroundColor(color: Int)`**  
  Updates the background color of the view.

- **`updateRadius(radius: Float)`**  
  Updates the uniform radius (corner radius) for all corners.

- **`updateRadius(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float)`**  
  Updates the radius for each corner individually.

- **`updateBackgroundBlur(blur: Float)`**  
  Applies a blur effect to the background with the specified intensity.

- **`updateBackgroundBlurType(blurType: BlurMaskFilter.Blur)`**  
  Defines the type of blur effect for the background (e.g., NORMAL, SOLID, OUTER, INNER).

- **`updateBackgroundRadiusHalf(enable: Boolean)`**  
  Enables or disables the "half radius" effect on the background.

---

## 2. Shadow Customization

- **`addBackgroundShadow(blurSize: Float, offsetX: Float, offsetY: Float, shadowColor: Int)`**  
  Adds a shadow with specified blur size, offset, and color.

- **`addBackgroundShadow(blurSize: Float, offsetX: Float, offsetY: Float, spread: Float, shadowColor: Int)`**  
  Adds a shadow with a spread effect in addition to the blur size and offset.

- **`removeBackgroundShadowLast()`**  
  Removes the last shadow in the list.

- **`removeBackgroundShadowFirst()`**  
  Removes the first shadow in the list.

- **`removeAllBackgroundShadows()`**  
  Clears all shadows from the list.

- **`removeBackgroundShadow(position: Int)`**  
  Removes the shadow at the specified position.

- **`updateBackgroundShadow(position: Int, shadow: Shadow)`**  
  Updates a specific shadow at a given position with a new `Shadow` object.

- **`updateBackgroundShadow(position: Int, blurSize: Float, offsetX: Float, offsetY: Float, color: Int)`**  
  Updates the shadow at a given position with new parameters: blur size, offset, and color.

- **`updateBackgroundShadow(position: Int, blurSize: Float, offsetX: Float, offsetY: Float, spread: Float, color: Int)`**  
  Updates the shadow with additional spread properties.

- **`updateBackgroundShadow(shadow: Shadow)`**  
  Updates the first shadow in the list with a new `Shadow` object.

- **`updateBackgroundShadow(blurSize: Float, offsetX: Float, offsetY: Float, color: Int)`**  
  Updates the first shadow with the provided blur size, offset, and color.

- **`updateBackgroundShadow(blurSize: Float, offsetX: Float, offsetY: Float, spread: Float, color: Int)`**  
  Updates the first shadow with blur size, offset, spread, and color.

---

## 3. Stroke Customization

- **`updateStrokeWidth(strokeWidth: Float)`**  
  Updates the width of the stroke.

- **`updateStrokeColor(color: Int)`**  
  Updates the color of the stroke.

- **`updateStrokeType(strokeType: StrokeType)`**  
  Updates the type of the stroke.  

- **`updateStrokeBlur(blur: Float)`**  
  Applies a blur effect to the stroke with the specified intensity.

- **`updateStrokeBlurType(blurType: BlurMaskFilter.Blur)`**  
  Defines the type of blur effect for the stroke.

---

## 4. Gradient Customization

- **`updateGradientColor(startColor: Int, centerColor: Int, endColor: Int)`**  
  Updates the gradient color with three colors: start, center, and end.

- **`updateGradientColor(startColor: Int, endColor: Int)`**  
  Updates the gradient color with two colors: start and end.

- **`updateGradientColors(colors: IntArray)`**  
  Updates the colors of the gradient.

- **`updateGradientPositions(positions: FloatArray)`**  
  Updates the positions of the gradient.

- **`updateGradientAngle(angle: Int)`**  
  Updates the angle of the gradient.

- **`updateLocalMatrix(matrix: Matrix?)`**  
  Applies a local transformation matrix to the gradient.

- **`updateGradientOffsetX(offset: Float)`**  
  Updates the horizontal offset of the gradient.

- **`updateGradientOffsetY(offset: Float)`**  
  Updates the vertical offset of the gradient.

- **`updateStrokeGradientColor(startColor: Int, centerColor: Int, endColor: Int)`**  
  Updates the stroke's gradient color with three colors.

- **`updateStrokeGradientColor(startColor: Int, endColor: Int)`**  
  Updates the stroke's gradient color with two colors.

- **`updateStrokeGradientColors(colors: IntArray)`**  
  Updates the stroke's gradient colors.

- **`updateStrokeGradientPositions(positions: FloatArray)`**  
  Updates the stroke's gradient positions.

- **`updateStrokeGradientAngle(angle: Int)`**  
  Updates the stroke's gradient angle.

- **`updateStrokeLocalMatrix(matrix: Matrix?)`**  
  Applies a local transformation matrix to the stroke's gradient.

- **`updateStrokeGradientOffsetX(offset: Float)`**  
  Updates the horizontal offset of the stroke's gradient.

- **`updateStrokeGradientOffsetY(offset: Float)`**  
  Updates the vertical offset of the stroke's gradient.

---

## 5. Helper Methods

- **`getGradientInfo(): Gradient?`**  
  Returns the current gradient settings applied to the view.

- **`getRadiusInfo(): Radius?`**  
  Returns the current radius settings applied to the view.

- **`getStrokeInfo(): Stroke?`**  
  Returns the current stroke settings applied to the view.

---

## Example Usage

```kotlin
// Set background color
view.updateBackgroundColor(Color.RED)

// Set corner radius
view.updateRadius(20f)

// Add a shadow with blur and offset
view.addBackgroundShadow(10f, 5f, 5f, Color.BLACK)

// Update stroke width
view.updateStrokeWidth(4f)

// Set gradient colors
view.updateGradientColor(Color.BLUE, Color.GREEN)
```

License
```
License
Copyright 2021 Jong Heon Son (sonsation)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

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

```xml
<com.sonsation.library.ShadowLayout
    android:id="@+id/shadow_layout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="10dp"
    app:background_color="#c8c8c8"
    app:background_radius="10dp"
    app:background_top_left_radius="10dp"
    app:background_top_right_radius="10dp"
    app:background_bottom_left_radius="10dp"
    app:background_bottom_right_radius="10dp"
    app:background_blur="10dp"
    app:background_blur_type="INNER"
    app:background_radius_half="true">

    <androidx.appcompat.widget.AppCompatTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="The early bird catches the worm."/>

</com.sonsation.library.ShadowLayout>
```

<p align="center"><img src="https://github.com/user-attachments/assets/ea2bde0d-06f3-46d3-ac91-3cc06e305ccd" width="400px"></p>


## Attributes

| Attribute                        | Description                                                                 |
|----------------------------------|-----------------------------------------------------------------------------|
| `app:background_color`           | Background color of the view. Example: `#c8c8c8`.                          |
| `app:background_radius`          | Radius for all corners. Example: `10dp`.                                   |
| `app:background_top_left_radius` | Radius for the top-left corner. Example: `10dp`.                           |
| `app:background_top_right_radius`| Radius for the top-right corner. Example: `10dp`.                          |
| `app:background_bottom_left_radius` | Radius for the bottom-left corner. Example: `10dp`.                     |
| `app:background_bottom_right_radius`| Radius for the bottom-right corner. Example: `10dp`.                   |
| `app:background_blur`            | Blur radius applied to the background. Example: `10dp`.                    |
| `app:background_blur_type`       | Type of blur effect. Options: `INNER`, `OUTER`, `SOLID`. Example: `INNER`.          |
| `app:background_radius_half`     | If `true`, sets the radius to half of the view's size for rounded effect.   |
  

## Reference

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

```xml
<com.sonsation.library.ShadowLayout
    android:id="@+id/shadow_layout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="10dp"
    app:shadow_blur="10dp"
    app:shadow_spread="10dp"
    app:shadow_color="#c8c8c8"
    app:shadow_offset_x="4dp"
    app:shadow_offset_y="4dp"
    app:shadow_array="{10,0,4,10,#c8c8c8}">

    <androidx.appcompat.widget.AppCompatTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="The early bird catches the worm."/>

</com.sonsation.library.ShadowLayout>
```

<p align="center"><img src="https://github.com/user-attachments/assets/2dd83e3b-14bb-4cc2-8d01-540b2f218848" width="400px"></p>

## Attributes
| Attribute              | Description                                                                                   |
|------------------------|-----------------------------------------------------------------------------------------------|
| `app:shadow_blur`      | The blur radius of the shadow. Example: `10dp`.                                               |
| `app:shadow_spread`    | The spread radius of the shadow, controlling its expansion or contraction. Example: `10dp`.   |
| `app:shadow_color`     | The color of the shadow. Example: `#c8c8c8`.                                                  |
| `app:shadow_offset_x`  | Horizontal offset of the shadow. Example: `4dp`.                                              |
| `app:shadow_offset_y`  | Vertical offset of the shadow. Example: `4dp`.                                                |
| `app:shadow_array`     | Allows configuring multiple shadows with a single attribute. Format: `{radius, offset_x, offset_y, spread, color}`. |

### Shadow Array Format
The `app:shadow_array` attribute allows you to define one or more shadow configurations. Each shadow is specified as:
app:shadow_array="{10,0,4,10,#c8c8c8}, {10,0,4,10,#000000}"

## Reference

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

```xml
<com.sonsation.library.ShadowLayout
    android:id="@+id/shadow_layout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="10dp"
    app:stroke_color="#c8c8c8"
    app:stroke_width="4dp"
    app:stroke_type="INSIDE"
    app:stroke_blur="10dp"
    app:stroke_blur_type="INNER"
    app:stroke_alpha="100">

    <androidx.appcompat.widget.AppCompatTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="The early bird catches the worm."/>

</com.sonsation.library.ShadowLayout>
```

<p align="center"><img src="https://github.com/user-attachments/assets/140b3bef-92b0-47f7-9ccf-0cb85cf54c2d" width="400px"></p>

## Attributes

| Attribute              | Description                                                                                                  |
|------------------------|--------------------------------------------------------------------------------------------------------------|
| `app:stroke_color`     | The color of the stroke. Supports any valid color value, such as HEX codes. Example: `#c8c8c8`.              |
| `app:stroke_width`     | The width of the stroke in density-independent pixels (dp). Example: `4dp`.                                  |
| `app:stroke_type`      | Defines where the stroke is drawn relative to the view boundary. Options: `INSIDE`, `CENTER`, `OUTSIDE`. Default: `CENTER`. |
| `app:stroke_blur`      | The blur radius applied to the stroke. Example: `10dp`.                                                      |
| `app:stroke_blur_type` | Type of blur applied to the stroke. Options: `INNER`, `OUTER`, `SOLID`. Default: `INNER`.                    |
| `app:stroke_alpha`     | The transparency level of the stroke, ranging from `0` (completely transparent) to `100` (fully opaque). Default: `100`. |

### Stroke Type
- **INSIDE**: The stroke is drawn inside the view boundary, reducing the available space for the content.  
- **CENTER**: The stroke is drawn evenly across the boundary edge (default).  
- **OUTSIDE**: The stroke is drawn outside the view boundary, extending outward.  

### Stroke Blur Type
- **INNER**: The blur is applied inside the stroke, making the outer edges sharp.  
- **OUTER**: The blur is applied outside the stroke, softening the outer edges.  
- **SOLID**: A solid stroke without any blur effect.

## Reference

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

- **`updateStrokeAlpha(alpha: Int)`**  
  Sets the transparency level of the stroke. Accepts a value between `0` (fully transparent) and `100` (fully opaque).

---

## 4. Gradient Customization

```xml
<com.sonsation.library.ShadowLayout
    android:id="@+id/shadow_layout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="10dp"
    app:gradient_angle="45"
    app:gradient_start_color="#ffffff"
    app:gradient_center_color="#c8c8c8"
    app:gradient_end_color="#000000"
    app:gradient_offset_x="4dp"
    app:gradient_offset_y="4dp">

    <androidx.appcompat.widget.AppCompatTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="The early bird catches the worm."/>

</com.sonsation.library.ShadowLayout>
```

<p align="center"><img src="https://github.com/user-attachments/assets/2724990b-d4ef-4b32-9fb7-7dd6faf88f05" width="400px"></p>

## Attributes

| Attribute                 | Description                                                                                             |
|---------------------------|---------------------------------------------------------------------------------------------------------|
| `app:gradient_angle`       | The angle of the gradient in degrees. Example: `45`.                                                     |
| `app:gradient_start_color` | The start color of the gradient. Example: `#ffffff`.                                                     |
| `app:gradient_center_color`| The center color of the gradient, if applicable. Example: `#c8c8c8`.                                      |
| `app:gradient_end_color`   | The end color of the gradient. Example: `#000000`.                                                       |
| `app:gradient_offset_x`    | The horizontal offset of the gradient. Example: `4dp`.                                                   |
| `app:gradient_offset_y`    | The vertical offset of the gradient. Example: `4dp`.                                                     |

```xml
<com.sonsation.library.ShadowLayout
    android:id="@+id/shadow_layout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="10dp"
    app:stroke_width="10dp"
    app:stroke_color="#000000"
    app:stroke_gradient_angle="45"
    app:stroke_gradient_start_color="#ffffff"
    app:stroke_gradient_center_color="#c8c8c8"
    app:stroke_gradient_end_color="#000000"
    app:stroke_gradient_offset_x="4dp"
    app:stroke_gradient_offset_y="4dp">

    <androidx.appcompat.widget.AppCompatTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="The early bird catches the worm."/>

</com.sonsation.library.ShadowLayout>
```

<p align="center"><img src="https://github.com/user-attachments/assets/bfd81f1f-03c4-4470-a23e-2bee7560df00" width="400px"></p>

## Attributes

| Attribute                          | Description                                                                                   |
|------------------------------------|-----------------------------------------------------------------------------------------------|
| `app:stroke_width`                 | The width of the stroke in density-independent pixels (dp). Example: `10dp`.                   |
| `app:stroke_color`                 | The color of the stroke. Supports any valid color value, such as HEX codes. Example: `#000000`. |
| `app:stroke_gradient_angle`        | The angle of the stroke gradient in degrees. Example: `45`.                                    |
| `app:stroke_gradient_start_color`  | The start color of the stroke gradient. Example: `#ffffff`.                                    |
| `app:stroke_gradient_center_color` | The center color of the stroke gradient, if applicable. Example: `#c8c8c8`.                    |
| `app:stroke_gradient_end_color`    | The end color of the stroke gradient. Example: `#000000`.                                      |
| `app:stroke_gradient_offset_x`     | The horizontal offset of the stroke gradient. Example: `4dp`.                                  |
| `app:stroke_gradient_offset_y`     | The vertical offset of the stroke gradient. Example: `4dp`.  

## Reference 

- **`updateGradientColor(startColor: Int, centerColor: Int, endColor: Int)`**  
  Updates the gradient color with three colors: start, center, and end.

- **`updateGradientColor(startColor: Int, endColor: Int)`**  
  Updates the gradient color with two colors: start and end.

- **`updateGradientColors(colors: IntArray?)`**  
  Updates the colors of the gradient.

- **`updateGradientPositions(positions: FloatArray?)`**  
  Updates the positions of the gradient.

- **`updateGradientAngle(angle: Int)`**  
  Updates the angle of the gradient.

- **`updateLocalMatrix(matrix: Matrix?)`**  
  Applies a local transformation matrix to the gradient.

- **`updateGradientShader(shader: GradientShader?)`**  
  Updates the shader of the gradient.

- **`updateGradientOffsetX(offset: Float)`**  
  Updates the horizontal offset of the gradient.

- **`updateGradientOffsetY(offset: Float)`**  
  Updates the vertical offset of the gradient.

- **`updateStrokeGradientColor(startColor: Int, centerColor: Int, endColor: Int)`**  
  Updates the stroke's gradient color with three colors.

- **`updateStrokeGradientColor(startColor: Int, endColor: Int)`**  
  Updates the stroke's gradient color with two colors.

- **`updateStrokeGradientColors(colors: IntArray?)`**  
  Updates the stroke's gradient colors.

- **`updateStrokeGradientPositions(positions: FloatArray?)`**  
  Updates the stroke's gradient positions.

- **`updateStrokeGradientAngle(angle: Int)`**  
  Updates the stroke's gradient angle.

- **`updateStrokeLocalMatrix(matrix: Matrix?)`**  
  Applies a local transformation matrix to the stroke's gradient.

- **`updateStrokeGradientShader(shader: GradientShader?)`**  
  Updates the stroke's gradient shader.

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

## License
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

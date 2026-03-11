uniform mat4 u_MVPMatrix;
uniform mat4 u_MVMatrix;// A constant representing the combined model/view matrix.

attribute vec4 a_Position;
attribute vec4 a_Color;
attribute vec2 a_TextureCoordinates;

varying vec3 v_Position;		// Interpolated position for this fragment.
varying vec4 v_Color;
varying vec2 v_TextureCoordinates;

void main() {
    // Transform the vertex into eye space.
	v_Position = vec3(u_MVMatrix * a_Position);

    v_Color = a_Color;
    // 纹理坐标 后续片段着色器需要
    v_TextureCoordinates = a_TextureCoordinates;
    // 齐次剪裁坐标
    gl_Position = u_MVPMatrix * a_Position;
}
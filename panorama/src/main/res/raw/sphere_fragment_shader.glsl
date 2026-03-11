precision mediump float; //中等 float 精度

varying vec4 v_Color;
varying vec2 v_TextureCoordinates;

uniform sampler2D u_Texture;

void main() {
    // u_Texture为纹理 v_TextureCoordinates为uv坐标
    gl_FragColor = texture2D(u_Texture, v_TextureCoordinates);
}

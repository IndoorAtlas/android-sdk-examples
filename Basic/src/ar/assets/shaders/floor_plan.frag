precision mediump float;

uniform sampler2D u_Texture;

varying vec2 v_TexCoord;

void main() {
    vec4 tex_color = texture2D(u_Texture, v_TexCoord);
    float alpha = min(0.8, tex_color.w);
    gl_FragColor = vec4(tex_color.xyz, alpha);
}
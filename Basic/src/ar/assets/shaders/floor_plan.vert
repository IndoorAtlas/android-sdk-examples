uniform mat4 u_ModelViewProjection;

attribute vec3 a_Position; // (x, y, z)
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;

void main() {
    v_TexCoord = a_TexCoord;
    // no normal: don't care about shading
    gl_Position = u_ModelViewProjection * vec4(a_Position, 1.0);
}

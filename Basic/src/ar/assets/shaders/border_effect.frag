precision mediump float;
varying vec2 v_TexCoord;
uniform sampler2D u_colorTexture;
//uniform sampler2D u_depthTexture;
uniform vec2 u_pixelDelta;

#define BORDER_HALF_RADIUS 2
const vec4 BORDER_COLOR = vec4(1,1,1,1);

void main() {
    int nBg = 0;
    int nFg = 0;
    vec4 selfColor;
    float maxAlpha = 0.0;
    for (int i=-BORDER_HALF_RADIUS; i <= BORDER_HALF_RADIUS; ++i) {
        for (int j=-BORDER_HALF_RADIUS; j <= BORDER_HALF_RADIUS; ++j) {
            // circular filter.. slightly less lookups and slightly less heavy
            if (i*i + j*j <= BORDER_HALF_RADIUS*BORDER_HALF_RADIUS) {
                // TODO: rather compare depth buffers for a nicer effect
                vec4 col = texture2D(u_colorTexture, v_TexCoord + u_pixelDelta * vec2(i, j));
                if (col.a < 0.01) {
                    nBg += 1;
                } else {
                    nFg += 1;
                }
                maxAlpha = max(col.a, maxAlpha);
                if (i == 0 && j == 0) selfColor = col;
            }
        }
    }

    //float borderness = float(min(nBg, nFg))/float(nFg+nBg) * 2.0;
    float borderness = 0.0;
    if (nBg != 0 && nFg != 0) borderness = 1.0;

    vec4 bcol = BORDER_COLOR;
    bcol.a = maxAlpha;
    gl_FragColor = borderness * bcol + (1.0 - borderness) * selfColor;
}

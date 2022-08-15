#ifdef GLES
precision   highp float;
#endif

uniform     mat4  u_mvp;
attribute   vec2  a_pos;

// factor to increase arrow size relative to scale
// that the size is not jumping when zoom level is changed
// uniform     float u_width;


void main() {
   
    gl_Position   = u_mvp * vec4(a_pos, 30.0, 1.0);
}

$$

#ifdef GLES
precision   highp float;
#endif

void main() {
   
    gl_FragColor  = vec4(0.0, 0.0, 0.0, 1.0);
}

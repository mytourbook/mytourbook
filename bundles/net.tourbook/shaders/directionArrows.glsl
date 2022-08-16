#ifdef GLES
precision   highp float;
#endif

uniform     mat4  u_mvp;
attribute   vec2  a_pos;

// factor to increase arrow size relative to scale
// that the size is not jumping when zoom level is changed
uniform     float u_width;

// This factor is multiplied with the color when painting the outline
// the outline is painted first with more width, then the core line with less width
// coreline = 1.0
// outline  0...1 is darker
//          1...2 is brighter
uniform float     uOutlineBrightness;
uniform vec4      uArrowColor;

// fragment values
varying vec2      v_st;
varying vec4      vFragmentColor;


void main() {
   
   gl_Position   = u_mvp * vec4(a_pos + (u_width * 1.0), 30.0, 1.0);

   // 0...2 -> -1...1
   float outlineBrightness01 = uOutlineBrightness - 1.0;
   
   vec3 vertexColorWithBrightness = outlineBrightness01 > 0.0
         
         // > 0 -> brighter
         ? uArrowColor.rgb + outlineBrightness01
         
         // < 0 -> darker
         : uArrowColor.rgb * uOutlineBrightness;

   vFragmentColor = vec4(vertexColorWithBrightness, uArrowColor.a);
}

$$

#ifdef GLES
precision   highp float;
#endif

varying vec2       v_st;
varying vec4       vFragmentColor;

void main() {
   
//  gl_FragColor  = vec4(0.0, 0.0, 0.0, 1.0);
    
    gl_FragColor = vFragmentColor;

}

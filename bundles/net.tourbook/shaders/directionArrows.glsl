#ifdef GLES
precision   highp float;
#endif

uniform     mat4  u_mvp;
attribute   vec4  a_pos;
attribute   vec3  a_ColorCoord; 

// factor to increase arrow size relative to scale
// that the size is not jumping when zoom level is changed
uniform     float u_width;

//uniform     vec4  uArrowColor;

// fragment values
varying vec2   v_st;
//varying vec4   vFragmentColor;
varying vec3   vColorCoord;
varying float  vArrowPart;

void main() {
   
   gl_Position   = u_mvp * vec4(a_pos.xyz + u_width, 1.0);

//   vFragmentColor = uArrowColor;
   vArrowPart     = a_pos.w;
   vColorCoord    = a_ColorCoord;
}

$$

#ifdef GLES
precision   highp float;
#endif

//varying vec2   v_st;
//varying vec4   vFragmentColor;
varying vec3   vColorCoord;
varying float  vArrowPart;

void main() {
   
   short colorCoordX = vColorCoord.x;
   float wireValue;
   
   if (colorCoordX > 0.0) { 
      wireValue = 0.9;
   } else {
      wireValue = 0.1;
   }
   
   if(vArrowPart == 0) {

      // it's a wing

      if (wireValue > 0.5) {
         
         // inside	
         //gl_FragColor = vec4(0.1, .1, .1, .7);
         gl_FragColor = vec4(0.91, .1, .1, .8);
         
      } else {
         // border
         gl_FragColor = vec4(.9, .2, .2, .997);
      }

   } else {

      // it's a fin

      if (wireValue > 0.5) {
         
         // inside	
         //gl_FragColor = vec4(.2, .2, .2, .7);
         gl_FragColor = vec4(0.1, .91, .1, .7);
         
      } else {
         // border
         gl_FragColor = vec4(.8, .8, .8, .997);
      }
   }
    

}

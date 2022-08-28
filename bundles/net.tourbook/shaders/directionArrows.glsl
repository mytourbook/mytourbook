#ifdef GLES
precision   highp float;
#endif

uniform     mat4  u_mvp;
attribute   vec4  a_pos;
attribute   vec3  a_ColorCoord; 

// fragment values
varying vec3   vColorCoord;
varying float  vArrowPart;

void main() {
   
   gl_Position   = u_mvp * vec4(a_pos.xyz, 1.0);

   vArrowPart     = a_pos.w;
   vColorCoord    = a_ColorCoord;
}

$$

#ifdef GLES
precision   highp float;
#endif

varying vec3   vColorCoord;
varying float  vArrowPart;

void main() {
      
   vec3 	normal		= vColorCoord;
   float f_thickness	= 0.0325;

   // see to which edge this pixel is the closest
   float f_closest_edge = min(normal.x, min(normal.y, normal.z)); 
   
   // calculate derivative (divide f_thickness by this to have the line width constant in screen-space)
   float f_width = fwidth(f_closest_edge); 

    // calculate alpha
   float wireValue = smoothstep(f_thickness, f_thickness + f_width, f_closest_edge); 
   
   if(vArrowPart == 0) {

      // it's a wing

      if (wireValue > 0.5) {
         
         // inside	
         gl_FragColor = vec4(.1, .1, .1, .7);
         
      } else {
         // border
         gl_FragColor = vec4(.99, .02, .02, .999);
      }

   } else {

      // it's a fin

      if (wireValue > 0.5) {
         
         // inside	
         gl_FragColor = vec4(.2, .2, .2, .7);
         
      } else {
         // border
         gl_FragColor = vec4(.8, .8, .8, .98);
      }
   }
}

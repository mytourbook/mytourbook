#ifdef GLES
precision   highp float;
#endif

uniform     mat4  u_mvp;
attribute   vec4  a_pos;
attribute   vec3  attrib_ColorCoord; 
uniform     float uni_OutlineWidth_Fin;
uniform     float uni_OutlineWidth_Wing;

// passthrough fragment values
varying vec3   pass_ColorCoord;
varying float  pass_ArrowPart;
varying float  pass_OutlineWidth_Fin;
varying float  pass_OutlineWidth_Wing;

void main() {
   
   gl_Position       = u_mvp * vec4(a_pos.xyz, 1.0);

   pass_ArrowPart          = a_pos.w;
   pass_ColorCoord         = attrib_ColorCoord;
   pass_OutlineWidth_Fin   = uni_OutlineWidth_Fin;
   pass_OutlineWidth_Wing  = uni_OutlineWidth_Wing;
}

$$

#ifdef GLES
precision   highp float;
#endif

varying vec3   pass_ColorCoord;
varying float  pass_ArrowPart;
varying float  pass_OutlineWidth_Fin;
varying float  pass_OutlineWidth_Wing;

void main() {
      
   vec3 	normal		= pass_ColorCoord;

   // see to which edge this pixel is the closest
   float closestEdge = min(normal.x, min(normal.y, normal.z)); 
   
   // calculate derivative (divide pass_OutlineWidth by this to have the line width constant in screen-space)
   float closestEdgeWidth = fwidth(closestEdge); 
   
   if(pass_ArrowPart == 0) {

      // it's a wing
   
      float wireValue = smoothstep(pass_OutlineWidth_Wing, pass_OutlineWidth_Wing + closestEdgeWidth, closestEdge); 

      if (wireValue > 0.5) {
         
         // inside	
         gl_FragColor = vec4(.1, .1, .1, .7);
         
      } else {
         // border
         gl_FragColor = vec4(.99, .02, .02, .999);
      }

   } else {

      // it's a fin

      float wireValue = smoothstep(pass_OutlineWidth_Fin, pass_OutlineWidth_Fin + closestEdgeWidth, closestEdge); 

      if (wireValue > 0.5) {
         
         // inside	
         gl_FragColor = vec4(.2, .2, .2, .7);
         
      } else {
         // border
         gl_FragColor = vec4(.8, .8, .8, .98);
      }
   }
}

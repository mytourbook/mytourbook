#ifdef GLES
precision   highp float;
#endif

attribute   vec4  a_pos;
attribute   vec3  attrib_ColorCoord; 

uniform  mat4  u_mvp;
uniform  vec4  uni_ArrowColors[4];  // 1:wing inside, 2:wing outline, 3:fin inside, 4:fin outline
uniform  vec2  uni_OutlineWidth;    // x:Wing, y:Fin
uniform  float uni_Vp2MpScale;      // viewport to map scale: 1.0...2.0

// passthrough fragment values
varying float  pass_ArrowPart;
varying vec4   pass_ArrowColors[4];
varying vec3   pass_ColorCoord;
varying vec2   pass_OutlineWidth;

void main() {
   
   gl_Position       = u_mvp * vec4(a_pos.xy, a_pos.z * uni_Vp2MpScale, 1.0);

   pass_ArrowPart    = a_pos.w;
   pass_ColorCoord   = attrib_ColorCoord;
   pass_OutlineWidth = uni_OutlineWidth;
   pass_ArrowColors  = uni_ArrowColors;
}

$$

#ifdef GLES
precision   highp float;
#endif

varying float  pass_ArrowPart;
varying vec4   pass_ArrowColors[4];
varying vec3   pass_ColorCoord;        // barycentric coordinate inside the triangle
varying vec2   pass_OutlineWidth;

void main() {
      
   // Source: https://stackoverflow.com/questions/137629/how-do-you-render-primitives-as-wireframes-in-opengl#answer-33004265

   // see to which edge this pixel is the closest
   float closestEdge = min(pass_ColorCoord.x, min(pass_ColorCoord.y, pass_ColorCoord.z)); 
   
   // calculate derivative (divide pass_OutlineWidth by this to have the line width constant in screen-space)
   float closestEdgeWidth = fwidth(closestEdge); 
   
   if(pass_ArrowPart == 0) {

      // it's a wing
   
      float wireValue = smoothstep(pass_OutlineWidth.x, pass_OutlineWidth.x + closestEdgeWidth, closestEdge); 

      if (wireValue > 0.5) {
         
         // inside	
         gl_FragColor = pass_ArrowColors[0];
         
      } else {
         // border
         gl_FragColor = pass_ArrowColors[1];
      }

   } else {

      // it's a fin

      float wireValue = smoothstep(pass_OutlineWidth.y, pass_OutlineWidth.y + closestEdgeWidth, closestEdge); 

      if (wireValue > 0.5) {
         
         // inside	
         gl_FragColor = pass_ArrowColors[2];
         
      } else {
         // border
         gl_FragColor = pass_ArrowColors[3];
      }
   }
}

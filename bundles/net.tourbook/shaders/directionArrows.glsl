#ifdef GLES
precision   highp float;
#endif

attribute   vec4  a_pos;
attribute   vec3  attrib_ColorCoord; 
attribute   float attrib_ArrowIndices; 

uniform  mat4  u_mvp;
uniform  float uni_Vp2MpScale;      // viewport to map scale: 1.0...2.0

// passthrough fragment values
varying float  pass_ArrowPart;
varying float  pass_ArrowIndices;
varying vec3   pass_ColorCoord;

void main() {
   
   gl_Position       = u_mvp * vec4(a_pos.xy, a_pos.z * uni_Vp2MpScale, 1.0);

   pass_ArrowPart    = a_pos.w;
   pass_ColorCoord   = attrib_ColorCoord;
   pass_ArrowIndices = attrib_ArrowIndices;
}

$$

#ifdef GLES
precision   highp float;
#endif

varying float  pass_ArrowPart;
varying float  pass_ArrowIndices;
varying vec3   pass_ColorCoord;     // barycentric coordinate inside the triangle

uniform vec4   uni_ArrowColors[4];  // 1:wing inside, 2:wing outline, 3:fin inside, 4:fin outline
uniform float  uni_GlowArrowIndex;
uniform float  uni_GlowState;
uniform vec2   uni_OutlineWidth;    // x:Wing, y:Fin

vec4 getWingColor() {
      
   // Source: https://stackoverflow.com/questions/137629/how-do-you-render-primitives-as-wireframes-in-opengl#answer-33004265

   // see to which edge this pixel is the closest
   float closestEdge = min(pass_ColorCoord.x, min(pass_ColorCoord.y, pass_ColorCoord.z)); 
   
   // calculate derivative (divide uni_OutlineWidth by this to have the line width constant in screen-space)
   float closestEdgeWidth = fwidth(closestEdge); 
   
   // fix it with 0.5 because one arrow is between 2 points, otherwise 2 arrows are partly glowed
   float glowArrowIndex = uni_GlowArrowIndex + 0.5;
   float glowIntensity = 1.0;
   float glowVisibility = 1.0;
   
   if (uni_GlowState > 0) {
      
      // glowing is enabled
      
      if (pass_ArrowIndices >= glowArrowIndex && pass_ArrowIndices < glowArrowIndex + 1.0) {
         
         // arrow index is glowed
      
         glowIntensity *= 6.5;
      
      } else {
         
         // arrow index is not glowed
         
         glowIntensity *= 0.2;
         
         glowVisibility = 0.1;
      }
   }

   
   if(pass_ArrowPart == 0) {

      // it's a wing
   
      float wireValue = smoothstep(uni_OutlineWidth.x, uni_OutlineWidth.x + closestEdgeWidth, closestEdge); 

      if (wireValue > 0.5) {
         
         // inside	
         return vec4(uni_ArrowColors[0].xyz * glowIntensity, uni_ArrowColors[0].w * glowVisibility);
         
      } else {
         // border
         return vec4(uni_ArrowColors[1].xyz * glowIntensity, uni_ArrowColors[1].w * glowVisibility);
      }

   } else {

      // it's a fin

      float wireValue = smoothstep(uni_OutlineWidth.y, uni_OutlineWidth.y + closestEdgeWidth, closestEdge); 

      if (wireValue > 0.5) {
         
         // inside	
         return vec4(uni_ArrowColors[2].xyz * glowIntensity, uni_ArrowColors[2].w * glowVisibility);
         
      } else {
         
         // border
         return vec4(uni_ArrowColors[3].xyz * glowIntensity, uni_ArrowColors[3].w * glowVisibility);
      }
   }
}

void main() {

   vec4 wingColor = getWingColor();  
   
   // output color to the default frame buffer default color attachment
   gl_FragData[0] = wingColor;
}

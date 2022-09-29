
in vec4  a_pos;
in vec3  attrib_ColorCoord;
in float attrib_ArrowIndices;

uniform  mat4  u_mvp;
uniform  float uni_Vp2MpScale;      // viewport to map scale: 1.0...2.0

// passthrough fragment values
out float  out_ArrowPart;
out float  out_ArrowIndex;
out vec3   out_ColorCoord;

void main() {
   
   gl_Position       = u_mvp * vec4(a_pos.xy, a_pos.z * uni_Vp2MpScale, 1.0);

   out_ArrowPart   = a_pos.w;
   out_ColorCoord  = attrib_ColorCoord;
   out_ArrowIndex	= attrib_ArrowIndices;
}


$$


in float out_ArrowPart;
in float out_ArrowIndex;
in vec3  out_ColorCoord;               // barycentric coordinate inside the triangle

out vec4 out_Color;

uniform vec4   uni_ArrowColors[4];     // 1:wing inside, 2:wing outline, 3:fin inside, 4:fin outline
uniform float  uni_GlowArrowIndex;
uniform float  uni_IsAnimate;
uniform vec2   uni_OutlineWidth;       // x:Wing, y:Fin

vec4 getWingColor() {

   // Source: https://stackoverflow.com/questions/137629/how-do-you-render-primitives-as-wireframes-in-opengl#answer-33004265

   // see to which edge this pixel is the closest
   float closestEdge = min(out_ColorCoord.x, min(out_ColorCoord.y, out_ColorCoord.z));
   
   // calculate derivative (divide uni_OutlineWidth by this to have the line width constant in screen-space)
   float closestEdgeWidth = fwidth(closestEdge); 

   // fix it with 0.5 because one arrow is between 2 points, otherwise 2 arrows are partly glowed
   float glowArrowIndex = uni_GlowArrowIndex + 0.5;
   float glowIntensity = 1.0;
   float glowVisibility = 1.0;
   
   if (uni_IsAnimate == 1.0) {

     // animation is enabled

     if (out_ArrowIndex >= glowArrowIndex && out_ArrowIndex < glowArrowIndex + 1.0) {

       // arrow index is glowed

       glowIntensity *= 6.5;

     } else {

       // arrow index is not glowed

       glowIntensity *= 0.2;

       glowVisibility = 0.1;
     }
   }

   
   if(out_ArrowPart == 0) {

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
   out_Color = wingColor;
}

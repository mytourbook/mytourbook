
in vec4  a_pos;
in vec3  attrib_ColorCoord;

uniform  mat4  u_mvp;
uniform  float uni_Vp2MpScale;      // viewport to map scale: 1.0...2.0

// passthrough fragment values
out float  pass_ArrowPart;
out vec3   pass_ColorCoord;

void main() {
   
   gl_Position    = u_mvp * vec4(a_pos.xy,
                                 a_pos.z  * uni_Vp2MpScale,
                                 1.0);

   pass_ArrowPart  = a_pos.w;
   pass_ColorCoord = attrib_ColorCoord;
}


$$


in float pass_ArrowPart;
in vec3  pass_ColorCoord;               // barycentric coordinate inside the triangle

out vec4 out_Color;

uniform vec4   uni_ArrowColors[4];     // 1:wing inside, 2:wing outline, 3:fin inside, 4:fin outline
uniform vec2   uni_OutlineWidth;       // x:Wing, y:Fin

vec4 getWingColor() {

   // Source: https://stackoverflow.com/questions/137629/how-do-you-render-primitives-as-wireframes-in-opengl#answer-33004265

   // see to which edge this pixel is the closest
   float closestEdge = min(pass_ColorCoord.x, min(pass_ColorCoord.y, pass_ColorCoord.z));
   
   // calculate derivative (divide uni_OutlineWidth by this to have the line width constant in screen-space)
   float closestEdgeWidth = fwidth(closestEdge); 
   
   if (pass_ArrowPart == 0) {

     // it's a wing

     float wireValue = smoothstep(uni_OutlineWidth.x, uni_OutlineWidth.x + closestEdgeWidth, closestEdge);

     if (wireValue > 0.5) {

       // inside
       return uni_ArrowColors[0];

     } else {

       // border
       return uni_ArrowColors[1];
     }

   } else {

     // it's a fin

      float wireValue = smoothstep(uni_OutlineWidth.y, uni_OutlineWidth.y + closestEdgeWidth, closestEdge);

     if (wireValue > 0.5) {

       // inside
       return uni_ArrowColors[2];

     } else {

       // border
       return uni_ArrowColors[3];
     }
   }
}

void main() {

   vec4 wingColor = getWingColor();  
   
   // output color to the default frame buffer default color attachment
   out_Color = wingColor;
}

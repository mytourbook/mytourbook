
const vec3 colors[] = vec3[]( vec3( 1.0, 0.0, 0.0),
                              vec3( 0.0, 1.0, 0.0),
                              vec3( 0.0, 0.0, 1.0));

in  vec4 attrib_Pos;

uniform  mat4  uni_MVP;
uniform  float uni_Vp2MpScale;      // viewport to map scale: 1.0...2.0

// passthrough fragment values
out vec3 pass_Color;

void main() {
   
   gl_Position = uni_MVP * vec4(attrib_Pos.xy, attrib_Pos.z * uni_Vp2MpScale, 1.0);

   pass_Color  = colors[gl_VertexID];
}


$$


in  vec3 pass_Color;
out vec4 out_Color;

void main() {
   
   out_Color = vec4(pass_Color, .9);
}

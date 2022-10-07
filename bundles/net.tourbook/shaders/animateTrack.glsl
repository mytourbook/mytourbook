
const vec3 colors[] = vec3[]( vec3( 1.0, 1.0, 1.0),
                              vec3( 0.0, 1.0, 0.0),
                              vec3( 0.0, 0.0, 1.0));

in vec4 attrib_Pos;

uniform mat4  uni_MVP;
uniform vec2  uni_AnimationPos;
uniform mat4  uni_AnimationMVP;

// current viewport map scale to comile time map scale: 1.0...2.0
uniform float uni_VpScale2CompileScale;

// passthrough fragment values
out vec3 pass_Color;

void main() {
   

   // rotate model to look into the forward direction
   vec4 rotatedModel = attrib_Pos * uni_AnimationMVP;

   // scale model to current map scale
   vec3 scaledModel = rotatedModel.xyz * (1.0 / uni_VpScale2CompileScale);

   gl_Position = uni_MVP * vec4(scaledModel.xy

                                // move (translate) model to the animation position
                                + uni_AnimationPos,

                                scaledModel.z,
                                1.0);

   pass_Color  = colors[gl_VertexID];
}


$$


in  vec3 pass_Color;
out vec4 out_Color;

void main() {
   
   out_Color = vec4(pass_Color, .9);
}

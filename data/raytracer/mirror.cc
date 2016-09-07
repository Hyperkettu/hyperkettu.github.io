#include "mirror.hh"

using namespace std;
using namespace cgmath;

mirror_shader::mirror_shader (){
  this->reflective = true;
 this->transparent = false;
}

vector_3d mirror_shader::bsdf (const surface_point& point,
        const vector_3d&     in_dir,
        const vector_3d&  /* out_dir */)
{
  cgmath::vector_3d c(1.0);
  return c;
}

void mirror_shader::sample_bsdf (const surface_point& point,
          unsigned             num_samples,
          const vector_3d&     out_dir,
          bsdf_samples& samples)
{
  samples.resize (num_samples);
  double cos_alpha = cgmath::dot(-out_dir, point.normal);

  // reflected ray
  vector_3d refl = cgmath::normalized(-out_dir - 2 * cos_alpha * point.normal);
  samples[0] = bsdf_sample(refl, cgmath::vec(1.0, 1.0, 1.0));

}

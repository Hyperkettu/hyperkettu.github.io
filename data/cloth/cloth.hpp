#ifndef CLOTH_HPP
#define CLOTH_HPP

#include "object.hpp"
#include "pointMass.hpp"
#include "string.hpp"

namespace EngineGL {

/**
 * Class cloth represents a cloth
 */
class Cloth : public EngineGL::Object {

public:
	// Default constructor
	Cloth();

	// Class destructor
	~Cloth();

	/** 
	 * Class constructor
	 * @param location 
	 * @param orientation
	 * @param texture manager
	 */
	Cloth(const glm::vec3& location, const glm::vec3& orientation, EngineGL::GLTexture* textureManager);


	/**
	 * Draws this object
	 * @param camera
	 * @param model view matrix
	 * @param id for shader
	 */
	virtual void draw(EngineGL::GLCamera* camera, glm::mat4& modelView, GLint id);

	/**
	 * This function updates object, NOTE: static objects might not need to execute this function TODO
	 * @param time difference
	 */
	virtual void update(GLfloat dt);

	std::vector<PointMass*> points; ///< point masses
	std::vector<String*> strings; ///< damper strings
	glm::vec3 gravity; ///< gravity vector
	GLfloat timer; ///< timer
};

}
#endif
#include "cloth.hpp"

namespace EngineGL {

Cloth::Cloth(){}

Cloth::~Cloth(){
}

Cloth::Cloth(const glm::vec3& location, const glm::vec3& orientation, EngineGL::GLTexture* textureManager) : Object(location, orientation, textureManager){
	// add textures 
	GLuint texture = textureManager->loadTextureFromBMP("cloth.bmp");
	textures.push_back(texture);

	// add geometry
	EngineGL::VertexArrayObject* v = new EngineGL::VertexArrayObject(3);

	std::vector<GLfloat> ver;

	int m = 25;
	int n = 25;

	GLfloat gy = -10.0f;
	gravity = glm::vec3(0, gy, 0.0f);

	GLfloat distance = 0.005f;
	GLfloat width = distance * (n-1)/2.0f;
	GLfloat height = distance * (m-1)/2.0f;
	GLfloat mass = 0.1f;

	for(GLint i = 0; i < m; i += 1) {
		for(GLint j = 0; j < n; j += 1){
			GLfloat x, y, z;
			x = (i-m/2)*distance;
			y = 0;
			z = (j-n/2)*distance;

			ver.push_back(x);
			ver.push_back(y);
			ver.push_back(z);

			if(j == 0 || j== n-1 || i == 0 || i == m-1)
				points.push_back(new PointMass(mass, x, y, z, true));
			else
				points.push_back(new PointMass(mass, x, y, z, false));
		} 
	}
	GLfloat k = 10000.0f;
	GLfloat beta = 2.8f;

	// add strings
	for(GLuint i = 0; i < m-1; i++){
		for(GLuint j = 0; j < n-1; j++){

			strings.push_back(new String(points[i*n+j], points[(i+1)*n+j], distance, k, beta));
			strings.push_back(new String(points[i*n+j], points[(i+1)*n+j+1], sqrt(2.0f)*distance, k, beta));
			strings.push_back(new String(points[i*n+j], points[i*n+j+1], distance, k, beta));

			if(j > 0)
				strings.push_back(new String(points[i*n+j], points[(i+1)*n+j-1], sqrt(2.0f)*distance, k, beta));
			
			if(i == m-2 && j < n-1)
				strings.push_back(new String(points[(i+1)*n+j], points[(i+1)*n+j+1], distance, k, beta));

			if(j == n-2 && i < m-1){
				strings.push_back(new String(points[i*n+j+1], points[(i+1)*n+j+1], distance, k, beta));
				strings.push_back(new String(points[i*n+j+1], points[(i+1)*n+j], sqrt(2.0f)*distance, k, beta));
			}
		}
	}
	
	std::vector<GLuint> indices; 

	// calculate indices
	for(int side = 0; side < m-1; side++){
		for(int i = 0; i < n-1; i++){
			// first triangle
			indices.push_back(n*side+i);
			indices.push_back(n*side+i+1);
			indices.push_back(n*(side+1)+i);
			// second triangle
			indices.push_back((n)*side+i+1);
			indices.push_back((n)*(side+1)+i+1);
			indices.push_back((n)*(side+1)+i);
		}
	}

	v->addVertices(0, 3, ver, GL_DYNAMIC_DRAW);

	// add texture coordinates
	std::vector<GLfloat> tex;

	for(int i = 0; i < m; i++)
		for(int j = 0; j < n; j++){
			tex.push_back(1.0f/static_cast<GLfloat>(m - 1.0f)*i);
			tex.push_back(1.0f - 1.0f/static_cast<GLfloat>(n - 1.0f)*j);
		}

	v->addIndices(indices);
	v->addTextureCoordinates(2, 2, tex, GL_DYNAMIC_DRAW);
	v->updateNormals(GL_DYNAMIC_DRAW);

	parts.push_back(v);	
	timer = 0;
}

void Cloth::draw(EngineGL::GLCamera* camera, glm::mat4& modelView, GLint id){

	glDisable(GL_CULL_FACE);

	glm::mat4 mat = model;
	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, textures[0]);
	modelView = camera->view() * mat;
	glUniformMatrix4fv(id, 1, GL_FALSE, reinterpret_cast<GLfloat*>(&modelView[0,0]));
	parts[0]->drawElements(GL_TRIANGLES);

	glEnable(GL_CULL_FACE);
}

void Cloth::update(GLfloat dt){

	timer += 0.002f;
	std::vector<GLfloat> vertex;

	// apply string forces
	for(GLuint i = 0; i < strings.size(); i++)
		strings[i]->applyForce();

	// for each point mass
	for(GLuint i = 0; i < points.size(); i++){
		PointMass* p = points[i];

		// apply gravity
		p->applyForce(gravity);
		
		p->update(dt);

		GLfloat x,y,z;

		x = p->location.x;
		y = p->location.y;
		z = p->location.z;

		vertex.push_back(x);
		vertex.push_back(y);
		vertex.push_back(z);
	}

	// update cloth structure
	parts[0]->addVertices(0, 3, vertex, GL_DYNAMIC_DRAW);
	parts[0]->updateNormals(GL_DYNAMIC_DRAW);

	updateLocation();
}

}
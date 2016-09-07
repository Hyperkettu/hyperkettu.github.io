#include <iostream>
#include <vector>
#include <fstream>
#include <string>
#include <sstream>

#include <GL/glew.h>
#include <GL/glut.h>

#include "extlib/glm/glm.hpp"
#include "extlib/glm/gtc/matrix_transform.hpp"
#include "extlib/glm/gtc/matrix_inverse.hpp"
#include "extlib/glm/gtc/type_ptr.hpp"

#include "Eigen\Dense"

#include "arc.h"
#include "node.h"

#define BUFFER_OFFSET(i) ((char *)NULL + (i))

typedef unsigned char BYTE;

// function prototypes
void readPLY(std::vector<GLfloat>& vertices, std::vector<GLuint>& indices);
void render();
void init();
char* loadFile(char *fname, GLint &fSize);
void special(int key, int x, int y);
void keyboard(unsigned char key, int x, int y);
void mouseMotion(int x, int y);
void mouseClicked(int button, int state, int x0, int y0);
void keyReleased(int key, int x, int y);
BYTE* loadBMP(std::string filename);
void reshape(int w, int h);
void calculateNormalsAndWriteToFile();
void readNormalsFromFile();
void loadShader(GLuint& var, std::string vsh, std::string fsh);
void toEigenMatrix(Eigen::Matrix4d& eigen, glm::mat4& matrix);
void toEigenMatrix(Eigen::Matrix4f& eigen, glm::mat4& matrix);
void toGLMMatrix(glm::mat4& matrix, Eigen::Matrix4d& eigen);
void initTree();
void drawLink(const Eigen::Matrix4d& mov, GLuint arrayId, GLuint indexId, std::vector<GLuint>& indices);
void evalArc(Arc* root);
void evalNode(Node& node, Eigen::Matrix4d& m);
Eigen::Vector4d f();
double error();
Eigen::Vector4d e();
Eigen::Vector3d JTe();
void jacobi();
bool iterate();
void direct();
bool ready;
Eigen::Matrix4f convert(const Eigen::Matrix4d& mat);

double lambda;

// IK model
Arc* root;

// keys
bool u, d, l, r;
int iterations;

// globals
std::vector<GLfloat> vertices;
std::vector<GLuint> indices;
int nvertices = 0; // number of vertices
int nindices = 0; // number of indices
GLuint VBO[6]; // vertex buffer object IDs
GLuint VAO[1]; // vertex array object IDs

GLuint texture[2];
std::vector<GLfloat> coord;
std::vector<GLfloat> normals;

int pmouseX = -1, pmouseY = -1; // previous mouse location
int wWidth = 1024, wHeight = 800; // window size

// matrices
Eigen::Matrix4f projection;
Eigen::Matrix4d modelView;
Eigen::Matrix4d model;
Eigen::Matrix4d View;
Eigen::Matrix4f temp;

Eigen::Matrix<double, 4, 3> J;

glm::mat4 view;

Eigen::Vector4d x;
Eigen::Vector4d y;

glm::vec3 eye;
glm::vec3 center;
glm::vec3 up;

GLuint pml, mvml, animl;

//shaders
GLuint phongShader, pointShader;


int main(int argc, char **argv) {
	glutInit(&argc, argv);
	glutInitWindowPosition(-1, -1);
	glutInitWindowSize(wWidth, wHeight);
	glutInitDisplayMode(GLUT_RGBA | GLUT_DOUBLE | GLUT_DEPTH);
	glutCreateWindow("IK");
	glewInit();
 
	readPLY(vertices, indices);

	init();

	glutDisplayFunc(render);
	glutSpecialUpFunc(keyReleased);
	glutSpecialFunc(special);
	glutKeyboardFunc(keyboard);
	glutIdleFunc(render);
	glutPassiveMotionFunc(mouseMotion);
	glutMouseFunc(mouseClicked);
	glutReshapeFunc(reshape);
	glutMainLoop();
}

void reshape(int w, int h) {
	if (h == 0)
		h = 1;
	wWidth = w;
	wHeight = h;
	GLfloat ratio = 1.0f * w / h;
	toEigenMatrix(projection, glm::perspective(60.0f, ratio, 0.1f, 100.0f));
	glViewport(0, 0, w, h);
}

void keyReleased(int key, int x, int y){
	

switch (key) {
	case 27:	// escape
		exit(0);
		x = x;
		y = y;
		break;
	case GLUT_KEY_UP: 
	
		u = false;
		

		break;
	case GLUT_KEY_LEFT:
		l = false;
		

		break;
	case GLUT_KEY_DOWN:
		d = false;
		

		break;
	case GLUT_KEY_RIGHT:
		r = false;
		

		break;		
	}
	
}

void init() {

	//init keys
	u = false;
	d = false;
	l = false;
	r = false;

	glClearColor(0.7, 0.8, 1.0, 1);
	glClearDepth(1);
	glDepthFunc(GL_LESS);
	glFrontFace(GL_CCW);
	glCullFace(GL_BACK);
	glEnable(GL_CULL_FACE);
	glEnable(GL_DEPTH_TEST);
	glEnable(GL_LIGHTING);

	// create vertex array
	glGenVertexArrays(1, &VAO[0]);
	glBindVertexArray(VAO[0]);

	// create vertex data for arm link
	glGenBuffers(1, &VBO[0]);
	glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
	glBufferData(GL_ARRAY_BUFFER, vertices.size()*sizeof(GLfloat), &vertices[0], GL_STATIC_DRAW);
	glVertexAttribPointer((GLuint)0, 3, GL_FLOAT, GL_FALSE, 0, 0);
	glEnableVertexAttribArray(0);

	// create normal data for arm link
	glGenBuffers(1, &VBO[3]);
	glBindBuffer(GL_ARRAY_BUFFER, VBO[3]);
	glBufferData(GL_ARRAY_BUFFER, normals.size()*sizeof(GLfloat), &normals[0], GL_STATIC_DRAW);
	glVertexAttribPointer((GLuint)2, 3, GL_FLOAT, GL_FALSE, 0, 0);
	glEnableVertexAttribArray(2);

	// create color data
	glGenBuffers(1, &VBO[4]);
	glBindBuffer(GL_ARRAY_BUFFER, VBO[4]);
	glBufferData(GL_ARRAY_BUFFER, vertices.size()*sizeof(GLfloat), &vertices[0], GL_STATIC_DRAW);
	glVertexAttribPointer((GLuint)1, 3, GL_FLOAT, GL_FALSE, 0, 0);
	glEnableVertexAttribArray(1);

	// release vertex array
	glBindVertexArray(0);

	// generate index data for link data
	glGenBuffers(1, &VBO[1]);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.size()*sizeof(GLuint), &indices[0], GL_STATIC_DRAW);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	
	// init matrices
	eye = glm::vec3(0.0f, 0.0f, 0.4f);
	center = glm::vec3(0.0f, 0.0f, -100.0f);
	up = glm::vec3(0.0f, 1.0f, 0.0f);
	view = glm::lookAt(eye, center, up);

	model = model.Identity();
	toEigenMatrix(projection, glm::perspective(60.0f, (float)wWidth/wHeight, 0.1f, 100.0f));

	loadShader(phongShader, "shader.vert", "shader.frag");

	initTree();

	x(0) = 0.1;
	x(1) = 0;
	x(2) = 0;
	x(3) = 1;

	y(0) = 0.3;
	y(1) = 0.02;
	y(2) = 0;
	y(3) = 1;

	lambda = 10.0 * error();
	iterations = 0;
	ready = false;
} 

void loadShader(GLuint& var, std::string vsh, std::string fsh) {
	// shader init
	GLuint fs, vs;
	char *vertexShader, *fragmentShader;
	vs = glCreateShader(GL_VERTEX_SHADER);
	fs = glCreateShader(GL_FRAGMENT_SHADER);
	GLint vlen, flen;
	vertexShader = loadFile((char*)vsh.c_str(), vlen);
	fragmentShader = loadFile((char*)fsh.c_str(), flen);
	const char *vv = vertexShader;
	const char *ff = fragmentShader;
	glShaderSource(vs, 1, &vv, &vlen);
	glShaderSource(fs, 1, &ff, &flen);

	// compile shaders
	GLint success;
	glCompileShader(vs);
	glGetShaderiv(vs, GL_COMPILE_STATUS, &success);
	if (!success) {
		int infoLogLen = 0, charsWritten = 0;
		char *infoLog = 0;
		glGetShaderiv(vs, GL_INFO_LOG_LENGTH, &infoLogLen);
		if (infoLogLen > 0) {
			infoLog = new char[infoLogLen];
			glGetShaderInfoLog(vs, infoLogLen, &charsWritten, infoLog);
			std::cout << infoLog << std::endl;
			delete[] infoLog;
		}
	}

	success = 0;
	glCompileShader(fs);
	glGetShaderiv(fs, GL_COMPILE_STATUS, &success);
	if (!success) {
		int infoLogLen = 0, charsWritten = 0;
		char *infoLog = 0;
		glGetShaderiv(fs, GL_INFO_LOG_LENGTH, &infoLogLen);
		if (infoLogLen > 0) {
			infoLog = new char[infoLogLen];
			glGetShaderInfoLog(fs, infoLogLen, &charsWritten, infoLog);
			std::cout << infoLog << std::endl;
			delete[] infoLog;
		}
	}

	var = glCreateProgram();

	glBindAttribLocation(var, 0, "pos");
	glBindAttribLocation(var, 1, "in_color");
	glBindAttribLocation(var, 2, "in_normal");

	glAttachShader(var, vs);
	glAttachShader(var, fs);
	glLinkProgram(var);
	GLint isLinked;
	glGetProgramiv(var, GL_LINK_STATUS, &isLinked);
	if (!isLinked) {
		int infoLogLen = 0, charsWritten = 0;
		char *infoLog = 0;
		glGetProgramiv(fs, GL_INFO_LOG_LENGTH, &infoLogLen);
		if (infoLogLen > 0) {
			infoLog = new char[infoLogLen];
			glGetProgramInfoLog(var, infoLogLen, &charsWritten, infoLog);
			std::cout << infoLog << std::endl;
			delete[] infoLog;
		}
	}

	pml = glGetUniformLocation(var, "projectionMatrix"); // projection matrix location
	mvml = glGetUniformLocation(var, "modelViewMatrix"); // model view matrix location

	delete[] vertexShader;
	delete[] fragmentShader;

	glUseProgram(var);

// Textures

	int width = 256, height = 256;
	BYTE* data = loadBMP("brick.bmp");
	BYTE* data2 = loadBMP("brick2.bmp");

	glGenTextures(1, &texture[0]);
	glGenTextures(1, &texture[1]);

	glBindTexture(GL_TEXTURE_2D, texture[0]);
	glEnable(GL_TEXTURE_2D);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, data);
	glGenerateMipmap(GL_TEXTURE_2D);

	glBindTexture(GL_TEXTURE_2D, texture[1]);
	glEnable(GL_TEXTURE_2D);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, data2);
	glGenerateMipmap(GL_TEXTURE_2D);

	free(data);
	free(data2);

	glBindVertexArray(VAO[0]);
	glGenBuffers(1, &VBO[2]);
	glBindBuffer(GL_ARRAY_BUFFER, VBO[2]);
	glBufferData(GL_ARRAY_BUFFER, coord.size() * sizeof(GLfloat), &coord[0], GL_STATIC_DRAW);
	GLint locTexcoords = glGetUniformLocation(var, "texture0");
	GLint locTexcoords2 = glGetUniformLocation(var, "texture1");
	glUniform1i(locTexcoords, 0);
	glUniform1i(locTexcoords2, 1);
	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, texture[0]);
	glActiveTexture(GL_TEXTURE1);
	glBindTexture(GL_TEXTURE_2D, texture[1]);
	glVertexAttribPointer(locTexcoords, 2, GL_FLOAT, GL_FALSE, 0, 0);
	glEnableVertexAttribArray(locTexcoords);
	glVertexAttribPointer(locTexcoords2, 2, GL_FLOAT, GL_FALSE, 0, 0);
	glEnableVertexAttribArray(locTexcoords2);
	glBindAttribLocation(var, locTexcoords, "inTexCoords");
}

// draws one link
void drawLink(const Eigen::Matrix4d& mov, GLuint arrayId, GLuint indexId, std::vector<GLuint>& indices){
	
	temp = convert(mov);

	glUniformMatrix4fv(mvml, 1, GL_FALSE, &temp(0,0));

	glEnableClientState(GL_VERTEX_ARRAY);

	glBindBuffer(GL_ARRAY_BUFFER, arrayId);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexId);

	glDrawElements(GL_TRIANGLES, indices.size(), GL_UNSIGNED_INT, BUFFER_OFFSET(0));

	GLUquadric *q = gluNewQuadric();
	gluSphere(q, 0.02, 20, 20);

	glDisableClientState(GL_VERTEX_ARRAY);
}

void mouseClicked(int button, int state, int x0, int y0){

	if(button == GLUT_LEFT && state == GLUT_DOWN){
		std::cout << "mouse: " << (x0-512.0f)/512.0f << ", " << (-y0+400.0f)/400.0f << std::endl;


		y(0) += x0*0.00001;
		y(1) += y0*0.00001;


	}
}

// convert eigen library matrix from double to float matrix
Eigen::Matrix4f convert(const Eigen::Matrix4d& mat){

	Eigen::Matrix4f c;

	for(int i = 0; i < 4; i++)
		for(int j = 0; j < 4; j++)
			c(i,j) = mat(i,j);

	return c;
}

// renders the kinematic chain tree
void render() {
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	toEigenMatrix(View, view);

	glUniformMatrix4fv(pml, 1, GL_FALSE, &projection(0,0));

	evalArc(root);

	glFlush();
	glutSwapBuffers();

	//iterate();

	if(u){

//		std::cout << y << std::endl;
	//	std::cout << iterations << std::endl;
//		std::cout << "----";

	//	std::cout << root->x.alpha << " ";
	//	std::cout << root->n.arcs[0].x.alpha << " ";
	//	std::cout << root->n.arcs[0].n.arcs[0].x.alpha << " ";
	//		std::cout << "----";

	}

	// moving calculations ----------------
	glm::vec3 direction = glm::normalize(center - eye);
	direction.x *= 0.001f;
	direction.y *= 0.001f;
	direction.z *= 0.001f;

	glm::vec3 left = glm::normalize(glm::cross(up, direction));

	left.x *= 0.001f;
	left.y *= 0.001f;
	left.z *= 0.001f;

	if(u){
	//	eye += direction;
	//	center += direction;

			y(1) += 0.002;
			direct();
		
		}

	if(d){
	//	eye -= direction;
	//	center -= direction;
			y(1) -= 0.002;
			direct();
	
	}

	if(r){
	//	eye -= left;
	//	center -= left;
		y(0) += 0.002;
		direct();
	}

	if(l){
	//	eye += left;
	//	center += left;
	
		y(0) -= 0.002;
		direct();
	}
	view = glm::lookAt(eye, center, up);

}

// converts GLM matrix to Eigen matrix
void toEigenMatrix(Eigen::Matrix4d& eigen, glm::mat4& matrix){
	for(int i = 0; i < 4; i++)
		for(int j = 0; j < 4; j++)
			eigen(i,j) = matrix[j][i];
}

void toEigenMatrix(Eigen::Matrix4f& eigen, glm::mat4& matrix){
	for(int i = 0; i < 4; i++)
		for(int j = 0; j < 4; j++)
			eigen(i,j) = matrix[j][i];
}

void toGLMMatrix(glm::mat4& matrix, Eigen::Matrix4d& eigen){
	for(int i = 0; i < 4; i++)
		for(int j = 0; j < 4; j++)
			matrix[j][i] = eigen(i,j);
}

void special(int key, int x, int y) {

	glm::vec3 direction = glm::normalize(center - eye);
	direction.x *= 0.1f;
	direction.y *= 0.1f;
	direction.z *= 0.1f;


	switch (key) {
	case 27:	// escape
		exit(0);
		x = x;
		y = y;
		break;
	case GLUT_KEY_UP:
		
		u = true;
		d = false;

		break;
	case GLUT_KEY_LEFT:
		l = true;
		r = false;

		break;
	case GLUT_KEY_DOWN:
		d = true;
		u = false;

		break;
	case GLUT_KEY_RIGHT:
		r = true;
		l = false;

		break;	
	case GLUT_KEY_F1: // blinn-phong
		loadShader(phongShader, "shader.vert", "shader.frag");
		break;
	case GLUT_KEY_F2: // point light shader
		loadShader(pointShader, "point.vert", "point.frag");
		break;

	}
	view = glm::lookAt(eye, center, up);
}

void keyboard(unsigned char key, int x, int y) {

	switch (key) {
	case 27:	// escape
		exit(0);
		x = x;
		y = y;
		break;
	}
	
}

void mouseMotion(int x, int y) {

}

// reads ply 3d modeling files
void readPLY(std::vector<GLfloat>& vertices, std::vector<GLuint>& indices) {
	std::ifstream file("link.ply");
	std::string line;
	if (!file.is_open()) {
		std::cout << "Unable to open link.ply" << std::endl;
		exit(1);
	}
	while (getline(file, line)) {
		std::stringstream ss(line);
		std::string word1, word2;
		ss >> word1;
		ss >> word2;
		if (word1 == "element" && word2 == "vertex") {
			ss >> nvertices;
			continue;
		}
		if (word1 == "element" && word2 == "face") {
			ss >> nindices;
			continue;
		}
		
		// read vertices and indices
		if (word1 == "end_header") {
			for (int i = 0; i < nvertices; ++i) {
				GLfloat coord;
				getline(file, line);
				std::stringstream ss2(line);
				ss2 >> coord; // x-coordination
				vertices.push_back(coord);
				ss2 >> coord; // y-coordination
				vertices.push_back(coord);
				ss2 >> coord; // z-coordination
				vertices.push_back(coord);
			}
			
			for (int i = 0; i < nindices; ++i) {
				GLuint index;
				getline(file, line);
				std::stringstream ss2(line);
				ss2 >> index; // no use
				ss2 >> index; // first vertex index
				indices.push_back(index);
				ss2 >> index; // second vertex index
				indices.push_back(index);
				ss2 >> index; // third vertex index
				indices.push_back(index);
			}			
		}
	}
	file.close();

	// texture coordinates
	for(unsigned int i = 0; i < vertices.size() / 3; i++) {

	coord.push_back(vertices[3*i] * 10);
	coord.push_back(vertices[3*i+1] * 10 - 0.45f);	

	}
	calculateNormalsAndWriteToFile();
	readNormalsFromFile();

}

// this function was almost stolen from https://gist.github.com/628320
char* loadFile(char *fname, GLint &fSize) {
	std::ifstream::pos_type size;
	char * memblock;
	std::string text;

	// file read based on example in cplusplus.com tutorial
	std::ifstream file (fname, std::ios::in|std::ios::binary|std::ios::ate);
	if (file.is_open()) {
		size = file.tellg();
		fSize = (GLuint) size;
		memblock = new char [size];
		file.seekg (0, std::ios::beg);
		file.read (memblock, size);
		file.close();
		#if DEBUG
		std::cout << "File " << fname << " loaded" << std::endl;
		#endif
		text.assign(memblock);
	}
	else {
		std::cout << "Unable to open file " << fname << std::endl;
		exit(1);
	}
	return memblock;
}

// loads bmp files
BYTE* loadBMP(std::string filename) {
	FILE* file = fopen(filename.c_str(), "rb");
	if (!file) {
		std::cout << "Unable to open " << filename << std::endl;
		exit(1);
	
	}
	char header[54];
	fread(header, 54, 1, file);
	int offset=*(unsigned int*)(header + 10);
	BYTE* temp = new BYTE[256*256*3]; // image size 256*256
	
	fseek(file,offset,SEEK_SET);
	fread(temp,256*256*3,1,file);
	for(int i = 0; i < 256*256*3; i += 3) {
		BYTE temp2 = temp[i];
		temp[i] = temp[i + 2];
		temp[i + 2] = temp2;
	}
	fclose(file);

	return temp;
}

// calculates normals for vertices and writes them to a file
void calculateNormalsAndWriteToFile() {
	std::ifstream check("normals.txt");
	if (check.is_open()) {
		std::cout << "Normals.txt already exists\nSkipping normal calculation" << std::endl;
		check.close();
		return;
	}

	for (unsigned int i = 0; i < vertices.size(); i+=3) {
		glm::vec3 currentVertex = glm::vec3(vertices[i], vertices[i+1], vertices[i+2]);
		glm::vec3 normal = glm::vec3(0.f, 0.f, 0.f);
		
		for (unsigned int e = 0; e < indices.size(); e+=3) {
			glm::vec3 a = glm::vec3(vertices[3*indices[e]], vertices[3*indices[e]+1],
								vertices[3*indices[e]+2]);
			glm::vec3 b = glm::vec3(vertices[3*indices[e+1]], vertices[3*indices[e+1]+1],
								vertices[3*indices[e+1]+2]);
			glm::vec3 c = glm::vec3(vertices[3*indices[e+2]], vertices[3*indices[e+2]+1],
								vertices[3*indices[e+2]+2]);
			if (currentVertex == a) {
				normal += glm::cross(a-b, c-b);
			}
			if (currentVertex == b) {
				normal += glm::cross(a-b, c-b);
			}
			if (currentVertex == c) {
				normal += glm::cross(a-b, c-b);
			}
		}
		normal = glm::normalize(normal);
		normals.push_back(normal.x);
		normals.push_back(normal.y);
		normals.push_back(normal.z);
		std::cout << "Calculating normals:    " << (int)((float)i / (float)vertices.size() * 100.0f) << "%\r";
	}

	std::ofstream file("normals.txt");
	if (file.is_open()) {
		for (unsigned int i = 0; i < normals.size(); i+=3) {
			file << normals[i] << ' ' << normals[i+1] << ' ' << normals[i+2] << '\n';
		}
		file.close();
	}
	else {
		std::cerr << "Unable to create normals.txt" << std::endl;
	}
}

// reads normals from file
void readNormalsFromFile() {
	std::string line;
	std::ifstream file("normals.txt");
	if (!file.is_open()) {
		std::cout << "Unable to open normals.txt" << std::endl;
		file.close();
		return;
	}
	while(getline(file, line)) {
		std::stringstream ss(line);
		GLfloat n1, n2, n3;
		ss >> n1;
		ss >> n2;
		ss >> n3;
		normals.push_back(n1);
		normals.push_back(n2);
		normals.push_back(n3);
	}
	file.close();
}

void initTree(){

	// init orientation matrices

	// first arm orientation rotates 90 degrees about z axis
	Eigen::Matrix4d trans1;
	toEigenMatrix(trans1, glm::translate(glm::mat4(1.0f), glm::vec3(0.1f, 0.0f, 0.0f)));

	Eigen::Matrix4d trans2;
	toEigenMatrix(trans2, glm::translate(glm::mat4(1.0f), glm::vec3(0.1f, 0.0f, 0.0f)));
	
	// init object parts
	Node rootNode(VBO[0], VBO[1]);
	Node n1(VBO[0], VBO[1]);
	Node n2(VBO[0], VBO[1]);
	Node zero11 = Node(-1, -1);
	Node zero12 = Node(-1, -1);
	Node zero21 = Node(-1, -1);
	Node zero22 = Node(-1, -1);

	root = new Arc(rootNode, model);
	//root->setRotation(Eigen::Matrix4f::Identity());
	Arc arc1(n1, trans1);
	Arc arc2(n2, trans2);
	root->setPosition(Eigen::Matrix4d::Identity());
	//arc1.setRotation(Eigen::Matrix4f::Identity());
	//arc2.setRotation(Eigen::Matrix4f::Identity());

	// init tree
	root->n.addArc(arc1);
	root->n.arcs[0].n.addArc(arc2);
	root->n.arcs[0].setAlpha(0);
	root->n.arcs[0].n.arcs[0].setAlpha(0);

	root->setAlpha(0);
}

// evauluates an arc in the tree
void evalArc(Arc* root){

	Eigen::Matrix4d mat = *root->m;
	mat *= root->M(root->x.alpha);
	evalNode(root->n, mat);
}


void evalNode(Node& node, Eigen::Matrix4d& m){

	Eigen::Matrix4d temp = m;

	// draw the link, if no zero length link
	if(node.obj != -1)
	drawLink(View * m, node.obj, node.index, indices);

	// for every arc evaluate tree recursively
	for(unsigned i = 0; i < node.arcs.size(); i++){
		temp = m * (*node.arcs[i].m);
		temp *= node.arcs[i].M(node.arcs[i].x.alpha);
		evalNode(node.arcs[i].n, temp);
	}
}

void frecursive(Node& node, Eigen::Matrix4d& m){

	// for every arc evaluate tree recursively
	for(unsigned i = 0; i < node.arcs.size(); i++){
		m = m * (*node.arcs[i].m);
		m *= node.arcs[i].M(node.arcs[i].x.alpha);
		frecursive(node.arcs[i].n, m);
	}
}

// computes end effector positions evaluating the tree
Eigen::Vector4d f(){

	Eigen::Matrix4d M = *root->m;
	M *= root->M(root->x.alpha);

	frecursive(root->n, M);
	return M * x;
}

// calculates square error
double error(){

	double e2 = 0.5 * e().transpose() * e();
	return e2;
}

void fdrecursive(Node& node, Eigen::Matrix4d& m){

	// for every arc evaluate tree recursively
	for(unsigned i = 0; i < node.arcs.size(); i++){
		m = m * (*node.arcs[i].m);
		m *= node.arcs[i].M(node.arcs[i].x.alpha);
		fdrecursive(node.arcs[i].n, m);
	}
}

// calculates the Jacobian matrix from the three angles
void jacobi(){

	// theta1
	Eigen::Matrix4d M = *root->m;
	Eigen::Vector4d row;

	M *= root->dMda(root->x.alpha);

	M *= (*root->n.arcs[0].m);
	M *= root->n.arcs[0].M(root->n.arcs[0].x.alpha);

	M *= (*root->n.arcs[0].n.arcs[0].m);
	M *= root->n.arcs[0].n.arcs[0].M(root->n.arcs[0].x.alpha);
	
	row = M * x;

	J(0, 0) = row(0);
	J(1, 0) = row(1);
	J(2, 0) = row(2);
	J(3, 0) = row(3);

	// theta2
	M = *root->m;
	M *= root->M(root->x.alpha);

	M *= (*root->n.arcs[0].m);
	M *= root->n.arcs[0].dMda(root->n.arcs[0].x.alpha);

	M *= (*root->n.arcs[0].n.arcs[0].m);
	M *= root->n.arcs[0].n.arcs[0].M(root->n.arcs[0].x.alpha);
	
	row = M * x;

	J(0, 1) = row(0);
	J(1, 1) = row(1);
	J(2, 1) = row(2);
	J(3, 1) = row(3);

	// theta3
    M = *root->m;
	M *= root->M(root->x.alpha);

	M *= (*root->n.arcs[0].m);
	M *= root->n.arcs[0].M(root->n.arcs[0].x.alpha);

	M *= (*root->n.arcs[0].n.arcs[0].m);
	M *= root->n.arcs[0].n.arcs[0].dMda(root->n.arcs[0].n.arcs[0].x.alpha);
	
	row = M * x;

	J(0, 2) = row(0);
	J(1, 2) = row(1);
	J(2, 2) = row(2);
	J(3, 2) = row(3);
}

// computes error vector, from end effector position to target vector
Eigen::Vector4d e(){
	return f() - y;
}

// computes jacobian (transposed) times error vector, ie gradient
Eigen::Vector3d JTe(){
	return J.transpose()*e();
}


void fdeltarecursive(Node& node, Eigen::Matrix4d& m){

	// for every arc evaluate tree recursively
	for(unsigned i = 0; i < node.arcs.size(); i++){
		m = m * (*node.arcs[i].m);
		m *= node.arcs[i].M(node.arcs[i].x.alphaPlusDelta);
		fdeltarecursive(node.arcs[i].n, m);
	}
}

Eigen::Vector4d fdelta(){

	Eigen::Matrix4d M = *root->m;
	M *= root->M(root->x.alphaPlusDelta);

	fdeltarecursive(root->n, M);
	return M * x;
}

// iterates steps according to Levenberg-Marquardt optimization method towards the target and quits
// iterating when we are close to the target
bool iterate(){

	// update jacobi for linearization
	jacobi();
	
	Eigen::Vector3d jte = JTe();

	// error gradient too big
	if(sqrt(jte.dot(jte)) < 1e-6)
	{
		std::cout << "Gradient too small." << std::endl;
		return false;
	}

	std::cout << "gradient: " << sqrt(jte.dot(jte)) << std::endl;

	// delta theta = (J^TJ - lambda*I)^(-1)J^Te;
	Eigen::Vector4d err = e();
	std::cout << "err: " << err.squaredNorm() << std::endl;
	if (err.squaredNorm() < 1e-6)
	{
		std::cout << "error too small." << std::endl;
		return false;
	}


	while (1)
	{
		Eigen::Vector3d delta;

		Eigen::Matrix3d JtJlambdaI = J.transpose()*J;
		JtJlambdaI += lambda * JtJlambdaI.Identity();
		
	std::cout << "lambda: " << lambda << std::endl;
	std::cout << "-determinant: " << JtJlambdaI.determinant() << std::endl;

		while(abs(JtJlambdaI.determinant()) < 1e-7){
			lambda *= 10.0;
			JtJlambdaI = J.transpose()*J;
			JtJlambdaI += lambda * JtJlambdaI.Identity();
		}

		Eigen::Matrix3d inv = JtJlambdaI.inverse();

		Eigen::Matrix<double, 3,4> temp = inv * J.transpose();
				
		delta = -temp * err;
		std::cout << "delta = " << delta << std::endl;
		if(delta.dot(delta) < 1e-8){
			std::cout << "delta too small." << std::endl;
			return false;
		}

		root->x.alphaPlusDelta = root->x.alpha + delta(0);
		root->n.arcs[0].x.alphaPlusDelta = root->n.arcs[0].x.alpha + delta(1);
		root->n.arcs[0].n.arcs[0].x.alphaPlusDelta = root->n.arcs[0].n.arcs[0].x.alpha + delta(2);

		Eigen::Vector4d fd = fdelta() - y;

		if(0.5*fd.transpose()*fd < 0.5*err.squaredNorm()) {
			root->x.alpha += delta(0);
			root->n.arcs[0].x.alpha += delta(1);
			root->n.arcs[0].n.arcs[0].x.alpha += delta(2);
			lambda /= 10.0;
			return true;
		}
		else 
		{
			lambda *= 10;
		}
	}
}

// this fuction directs the end effector towards the the target using Levenberg-Marquardt optimization method
void direct(){

	iterations = 0;
//	lambda = 100000000000.0f * error();
	lambda = 1e-03;
//	std::cout << "Start iteration." << std::endl;
	while(iterate()){
		iterations++;
	}

}
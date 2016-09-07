#ifndef ARC_H_
#define ARC_H_

#include "Eigen\Dense"

#include "node.h"
#include "variable.h"

#include <iostream>

/**
 * This class represents an arc
 */

class Arc {

public:
	// default constructor
	Arc(){}

	// constructor
	Arc(const Node& node, Eigen::Matrix4d& mat) : n(node), m(new Eigen::Matrix4d(mat)){
		rot = new Eigen::Matrix4d(Eigen::Matrix4d::Identity());
	}

	Arc(const Arc& arc){
	
		n = Node(arc.n.obj, arc.n.index);
		m = new Eigen::Matrix4d(*arc.m);
		rot = new Eigen::Matrix4d(*arc.rot);
	}

	Arc& operator=(const Arc& arc){
	
		n = Node(arc.n.obj, arc.n.index);
		m = new Eigen::Matrix4d(*arc.m);
		rot = new Eigen::Matrix4d(*arc.rot);

		return *this;
	}

	~Arc(){
		if(m != 0){
			delete m;
		}
	}

	void setPosition(const Eigen::Matrix4d& matrix){
		*m = matrix;
	}

	void setRotation(const Eigen::Matrix4d& rotation){
		*rot = rotation;
	}

	// generate a rotation matrix based on alpha around z axis
	Eigen::Matrix4d M (double alpha){
		
		Eigen::Matrix4d rot;

		rot(0,0) = cos(alpha); rot(0,1) = -sin(alpha); rot(0,2) = 0; rot(0,3) = 0;
		rot(1,0) = sin(alpha); rot(1,1) = cos(alpha); rot(1,2) = 0; rot(1,3) = 0;
		rot(2,0) = 0; rot(2,1) = 0; rot(2,2) = 1; rot(2,3) = 0;
		rot(3,0) = 0; rot(3,1) = 0; rot(3,2) = 0; rot(3,3) = 1;

		return rot;
	}

	
	// generate a rotation derivative matrix based on alpha
	Eigen::Matrix4d dMda (double alpha){
		
		Eigen::Matrix4d rot;

		rot(0,0) = -sin(alpha); rot(0,1) = -cos(alpha); rot(0,2) = 0; rot(0,3) = 0;
		rot(1,0) = cos(alpha); rot(1,1) = -sin(alpha); rot(1,2) = 0; rot(1,3) = 0;
		rot(2,0) = 0; rot(2,1) = 0; rot(2,2) = 0; rot(2,3) = 0;
		rot(3,0) = 0; rot(3,1) = 0; rot(3,2) = 0; rot(3,3) = 0;

		return rot;
	}

	void setAlpha(double alpha){
		x.alpha = alpha;
	}

	Node n;
	Eigen::Matrix4d* m;
	Eigen::Matrix4d* rot;
	Variable x;
};


#endif /* ARC_H_ */

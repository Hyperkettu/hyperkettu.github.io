import { vec3, mat4, vec4 } from 'gl-matrix';
import { Camera } from './camera';
import { Viewport } from './context';
import { Ray } from './ray';
import * as math from './util/math';
import { Triangle } from './triangle';
import { Sphere } from './sphere';
import { AABB } from './aabb';
import { SceneGraph } from './scenegraph';
import { MeshComponent } from './meshcomponent';
import { SceneNode } from './scenenode';
import { VertexBase } from './vertex';

export type HitPolicy = 'any' | 'closest';

export class Picker {

	constructor(viewport: Viewport, hitPolicy: HitPolicy) {
		this.viewport = viewport;
		this.hitPolicy = hitPolicy;
		this.pickerRadius = 1;
		this.distance = Number.MAX_SAFE_INTEGER;

		this.triangle = new Triangle(
			vec3.fromValues(-5, 0, -5),
			vec3.fromValues(5, 0, -5),
			vec3.fromValues(5, 0, 5));

		this.invTransform = mat4.create();
		this.localRay = new Ray(vec3.fromValues(0,0,0), vec3.fromValues(1,0,0));
		this.direction = vec4.create();

		this.sphere = new Sphere(vec3.fromValues(5, 0, 5), 5);

		this.aabb = new AABB(vec3.fromValues(-5, -5, -5), vec3.fromValues(5, 5, 5));
	}

	generateScreenRayFromCamera(camera: Camera, x: number, y: number) {

		const pointNear = vec3.fromValues(x, y, 0);
		const pointFar = vec3.fromValues(x, y, 1);
		const near = camera.unproject(this.viewport, pointNear);
		const far = camera.unproject(this.viewport, pointFar);

		const rayDir = vec3.create();
		vec3.sub(rayDir, far, near);
		vec3.normalize(rayDir, rayDir);

		return new Ray(camera.position, rayDir);
	}

	select(camera: Camera, x: number, y: number, world: SceneGraph) {
		const hitInfo = new HitInfo();
		hitInfo.hit = false;

		const hitObjects: SceneNode[] = [];

		const ray = this.generateScreenRayFromCamera(camera, x, y);

		const setAttributes = (node: SceneNode, info: HitInfo) => {
			hitInfo.hitObject = node;
			hitInfo.hit = true;
			hitInfo.hitPoint = info.hitPoint;
			hitInfo.normal = info.normal;
		};

		// add frustum and zone culling
		world.forEach(node => {
			const comp = node.getComponent('meshComponent') as MeshComponent<VertexBase>;
			if(comp && comp.mesh) {
				const info = new HitInfo();
				mat4.invert(this.invTransform, node.transform.world);
				vec3.transformMat4(this.localRay.origin, ray.origin, this.invTransform);
				this.direction = vec4.fromValues(ray.direction[0], ray.direction[1], ray.direction[2], 0);
				vec4.transformMat4(this.direction, this.direction, this.invTransform);
				this.localRay.direction = vec3.fromValues(this.direction[0], this.direction[1], this.direction[2]);
				if(comp.mesh.boundingVolume.intersects(this.localRay, info)) {
					if(this.hitPolicy === 'any') {
						setAttributes(node, info);
						return info;
					} else if(this.hitPolicy === 'closest') {
						hitObjects.push(node);
						const distance = vec3.distance(info.hitPoint, camera.position);
						if(distance < this.distance) {
							this.distance = distance;
							setAttributes(node, info);
						}
					}
				}
			}
		});

		let dist = Number.MAX_SAFE_INTEGER;

		if(this.hitPolicy === 'closest') {
			for(let obj of hitObjects) {
				const distance = vec3.distance(obj.transform.position, camera.position);
				if(distance < dist) {
					dist = distance;
					hitInfo.hitObject = obj;
				}
				
			}
		}

		return hitInfo;
	}

	castRay(camera: Camera, x: number, y: number, world: SceneGraph) {
		
		const hitInfo = new HitInfo();
		hitInfo.hit = false;
		const ray = this.generateScreenRayFromCamera(camera, x, y);

		const setAttributes = (node: SceneNode, info: HitInfo) => {
			hitInfo.hitObject = node;
			hitInfo.hit = true;
			hitInfo.hitPoint = info.hitPoint;
			hitInfo.normal = info.normal;
		};

		world.forEach(node => {

			const meshComponent = node.getComponent('meshComponent') as MeshComponent<VertexBase>;
			if(meshComponent && meshComponent.mesh) {
				const info = new HitInfo();
				for(let index = 0 ; index < meshComponent.mesh.getTriangleCount(); index++) {
					if(math.rayInterectsTriangle(ray, meshComponent.mesh.getTriangle(index), info)) {

						if(this.hitPolicy === 'any') {
							setAttributes(node, info);
							return hitInfo;
						} else if(this.hitPolicy === 'closest') {
							const distance = vec3.distance(info.hitPoint, camera.position);
							if(distance < this.distance) {
								this.distance = distance;
								setAttributes(node, info);
							}
						}
					}
				}
			}
		});
		return hitInfo;
	}

	invTransform: mat4;
	localRay: Ray;
	direction: vec4;

	pickerRadius: number;
	hitPolicy: 'any' | 'closest';

	distance: number;
	aabb: AABB;
	sphere: Sphere;
	triangle: Triangle;
	viewport: Viewport;
}

export class HitInfo {
	constructor() {
		this.hit = false;
	}
	hitPoint: vec3;
	hit: boolean;
	normal: vec3;
	hitObject: SceneNode;
}
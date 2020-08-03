import { DepthTexture } from './depthtexture';
import { vec3, mat4, vec4 } from 'gl-matrix';
import { DEG_TO_RAD } from './util/math';
import { Renderer, ShadowPass } from './glrenderer';
import { RenderTargetState } from './rendertarget';
import { Layer } from './batchrenderer';
import * as shader from './shadermanager';
import { ShaderType } from './shader';
import { TestScene } from './testscene';
import * as texture from './texturemanager';
import { ConstantBuffers } from './constantbuffers';
import { SceneNode } from './scenenode';

export class PointLightShadowMap {

	constructor(gl: WebGL2RenderingContext, width: number, height: number, position: vec3, radius: number) {
		this.width = width;
		this.height = height;
		this.position = position;
		this.radius = radius;
		this.near = 0.01;

		this.captureProjection = mat4.create();
		mat4.perspective(this.captureProjection, DEG_TO_RAD * 90.0, 1.0, this.near, this.radius);

		this.shadowCubeMap = DepthTexture.createRenderTargetCubeMap(gl, width, height);

		this.excludeFromShadowMapNodes = [];
	}

	excludeNodesFromShadowMap(renderer: Renderer, nodeNames: string[]) {
		for (let name of nodeNames) {
			this.excludeFromShadowMapNodes.push(renderer.currentScene.sceneGraph.find(name));
		}
	}

	setRadius(radius: number) {
		this.radius = radius;
		this.captureProjection = mat4.create();
		mat4.perspective(this.captureProjection, DEG_TO_RAD * 90.0, 1.0, this.near, this.radius);
	}

	setNear(near: number) {
		this.near = near;
		this.captureProjection = mat4.create();
		mat4.perspective(this.captureProjection, DEG_TO_RAD * 90.0, 1.0, this.near, this.radius);
	}

	initCaptureMatrices() {
		this.captureViews = [];
		let view = mat4.create();
		let target = vec3.fromValues(1, 0, 0);
		vec3.add(target, this.position, target);
		mat4.lookAt(view, this.position, target, vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
		view = mat4.create();
		target = vec3.fromValues(-1, 0, 0);
		vec3.add(target, this.position, target);
		mat4.lookAt(view, this.position, target, vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
		view = mat4.create();
		target = vec3.fromValues(0, 1, 0);
		vec3.add(target, this.position, target);
		mat4.lookAt(view, this.position, target, vec3.fromValues(0, 0, 1));
		this.captureViews.push(view);
		view = mat4.create();
		target = vec3.fromValues(0, -1, 0);
		vec3.add(target, this.position, target);
		mat4.lookAt(view, this.position, target, vec3.fromValues(0, 0, -1));
		this.captureViews.push(view);
		view = mat4.create();
		target = vec3.fromValues(0, 0, 1);
		target = vec3.add(target, this.position, target);
		mat4.lookAt(view, this.position, target, vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
		view = mat4.create();
		target = vec3.fromValues(0, 0, -1);
		target = vec3.add(target, this.position, target);
		mat4.lookAt(view, this.position, target, vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
	}

	render(renderer: Renderer, index: number) {
		const gl = renderer.gl;

		gl.colorMask(false, false, false, false);

		this.initCaptureMatrices();

		const rts = new RenderTargetState(gl, { x: 0, y: 0, width: this.width, height: this.height });
		renderer.context.renderTargetBegin(rts);

		for (let node of this.excludeFromShadowMapNodes) {
			node.enabled = false;
		}

		ConstantBuffers.matricesPerFrame.update(gl, 'projection', this.captureProjection);

		ConstantBuffers.generalData.update(gl, 'dataVec1', vec4.fromValues(this.position[0], this.position[1], this.position[2], 0));
		ConstantBuffers.generalData.update(gl, 'dataVec2', vec4.fromValues(0.01, this.radius, 0, 0));
		ConstantBuffers.generalData.sendToGPU(gl);

		for (let face = 0; face < 6; face++) {
			ConstantBuffers.matricesPerFrame.update(gl, 'view', this.captureViews[face]);
			ConstantBuffers.matricesPerFrame.sendToGPU(gl);
			rts.setRenderTargetCubemapFace(gl, face, this.shadowCubeMap);
			renderer.context.clear(gl.DEPTH_BUFFER_BIT);
			renderer.resolveVisibility(renderer.currentScene);
			renderer.batchRenderer.flushSortedArray(renderer, Layer.OPAQUE, ShadowPass.POINT_LIGHT);
			renderer.batchRenderer.flushSortedArray(renderer, Layer.TRANSPARENT, ShadowPass.POINT_LIGHT);
		}
		
		for (let face = 0; face < 6; face++) {
			rts.setRenderTargetCubemapFace(gl, face, null);
		}

		renderer.context.renderTargetEnd();
		renderer.shaderTech = null;
		renderer.shader = null;
		renderer.materialID = null;
		gl.colorMask(true, true, true, true);

		for (let node of this.excludeFromShadowMapNodes) {
			node.enabled = true;
		}


		texture.setDepthTexture(`pointLightShadowMap${index}`, this.shadowCubeMap);
	}

	captureProjection: mat4;
	captureViews: mat4[];
	width: number;
	height: number;
	near: number;
	radius: number;
	position: vec3;
	shadowCubeMap: DepthTexture;

	excludeFromShadowMapNodes: SceneNode[];
}
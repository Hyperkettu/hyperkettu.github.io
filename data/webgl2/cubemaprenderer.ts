import { Texture } from './texture';
import { Renderer } from './glrenderer';
import { RenderTargetState } from './rendertarget';
import { ShaderType } from './shader';
import * as shader from './shadermanager';
import { DEG_TO_RAD } from './util/math';
import { mat4, vec3, vec4 } from 'gl-matrix';
import * as mesh from './meshmanager';
import { VertexArrayObject } from './vertexarrayobject';
import * as texture from './texturemanager';
import { ConstantBuffers } from './constantbuffers';

export class CubeMapRenderer {
	constructor(gl: WebGL2RenderingContext) {
		this.initCaptureViews();

		const vertices = mesh.GenerateUnitCubeVertices();
		this.bufferData = VertexArrayObject.GenerateUnitCubeVertexArrayObject(gl, vertices);
	}

	initCaptureViews() {
		this.captureViews = [];

		let view = mat4.create();
		mat4.lookAt(view, vec3.fromValues(0, 0, 0), vec3.fromValues(1, 0, 0), vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
		view = mat4.create();
		mat4.lookAt(view, vec3.fromValues(0, 0, 0), vec3.fromValues(-1, 0, 0), vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
		view = mat4.create();
		mat4.lookAt(view, vec3.fromValues(0, 0, 0), vec3.fromValues(0, 1, 0), vec3.fromValues(0, 0, 1));
		this.captureViews.push(view);
		view = mat4.create();
		mat4.lookAt(view, vec3.fromValues(0, 0, 0), vec3.fromValues(0, -1, 0), vec3.fromValues(0, 0, -1));
		this.captureViews.push(view);
		view = mat4.create();
		mat4.lookAt(view, vec3.fromValues(0, 0, 0), vec3.fromValues(0, 0, 1), vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
		view = mat4.create();
		mat4.lookAt(view, vec3.fromValues(0, 0, 0), vec3.fromValues(0, 0, -1), vec3.fromValues(0, -1, 0));
		this.captureViews.push(view);
	}

	renderCube(gl: WebGL2RenderingContext) {
		Renderer.numDrawCallsPerFrame++;
		gl.bindVertexArray(this.bufferData.vertexArrayObject);
		gl.drawArrays(gl.TRIANGLES, 0, 36);
		gl.bindVertexArray(null);
	}

	generateIrradianceMap(renderer: Renderer) {

		const gl = renderer.gl;

		// generate 64x64 cubemap render target for irradiance (low frequency)
		const irradianceCubemapSize = 64;
		this.irradianceMap = Texture.createRenderTarget(gl, gl.RGBA32F, gl.RGBA,
			irradianceCubemapSize, irradianceCubemapSize, gl.FLOAT, gl.LINEAR, true);

		const rts = new RenderTargetState(gl, { x: 0, y: 0, width: irradianceCubemapSize, height: irradianceCubemapSize });
		rts.addColorTarget(gl, 0, this.irradianceMap);

		const irradianceShader = shader.GetShader(ShaderType.IRRADIANCE);
		irradianceShader.use(gl);

		ConstantBuffers.generalData.update(gl, 'dataVec1', vec4.fromValues(renderer.skybox.intensity, 0, 0, 0));
		ConstantBuffers.generalData.sendToGPU(gl);

		// use skybox as environment map
		irradianceShader.setSamplerTexture(gl, 'environmentMap', renderer.skybox.skyboxTextureCubeMap, 0);

		// 90 degrees perspective projection
		let captureProjection: mat4 = mat4.create();
		mat4.perspective(captureProjection, DEG_TO_RAD * 90.0, 1.0, 0.1, 10.0);
		ConstantBuffers.matricesPerFrame.update(gl, 'projection', captureProjection);

		renderer.context.renderTargetBegin(rts);
		// render each face of the cube
		for (let cubeMapFaceIndex = 0; cubeMapFaceIndex < 6; cubeMapFaceIndex++) {
			ConstantBuffers.matricesPerFrame.update(gl, 'view', this.captureViews[cubeMapFaceIndex]);
			ConstantBuffers.matricesPerFrame.sendToGPU(gl);
			renderer.context.renderToCubeMapFace(0, cubeMapFaceIndex, 0, this.irradianceMap, () => {

				gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
				this.renderCube(gl);

			});
		}

		renderer.context.renderTargetEnd();

		texture.setTexture('irradianceMap', this.irradianceMap);
	}

	generatePrefilterMap(renderer: Renderer) {

		const gl = renderer.gl;

		const prefilterCubeMapSize = 512;
		this.prefilterMap = Texture.createRenderTarget(gl, gl.RGBA32F, gl.RGBA,
			prefilterCubeMapSize, prefilterCubeMapSize, gl.FLOAT, gl.LINEAR_MIPMAP_LINEAR, true);

		const rts = new RenderTargetState(gl, { x: 0, y: 0, width: prefilterCubeMapSize, height: prefilterCubeMapSize });
		rts.addColorTarget(gl, 0, this.prefilterMap);

		const prefilterShader = shader.GetShader(ShaderType.PREFILTER_ENV_MAP);
		prefilterShader.use(gl);

		// use skybox as environment map
		prefilterShader.setSamplerTexture(gl, 'envMap', renderer.skybox.skyboxTextureCubeMap, 0);

		// 90 degrees perspective projection
		let captureProjection: mat4 = mat4.create();
		mat4.perspective(captureProjection, DEG_TO_RAD * 90.0, 1.0, 0.1, 10.0);
		ConstantBuffers.matricesPerFrame.update(gl, 'projection', captureProjection);

		renderer.context.renderTargetBegin(rts);

		const maxMipLevels = 5;

		for (let mip = 0; mip < maxMipLevels; mip++) {
			// resize framebuffer according to mip-level size
			const mipWidth = prefilterCubeMapSize >> mip;
			const mipHeight = prefilterCubeMapSize >> mip;
			rts.resize(gl, mipWidth, mipHeight);

			const roughness = mip / (maxMipLevels - 1);
			ConstantBuffers.generalData.update(gl, 'dataVec1', vec4.fromValues(roughness, renderer.skybox.intensity, 0, 0));
			ConstantBuffers.generalData.sendToGPU(gl);

			// render each face of the cube one each mip level
			for (let cubeMapFaceIndex = 0; cubeMapFaceIndex < 6; cubeMapFaceIndex++) {
				ConstantBuffers.matricesPerFrame.update(gl, 'view', this.captureViews[cubeMapFaceIndex]);
				ConstantBuffers.matricesPerFrame.sendToGPU(gl);
				renderer.context.renderToCubeMapFace(0, cubeMapFaceIndex, mip, this.prefilterMap, () => {

					gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
					this.renderCube(gl);

				});
			}
		}

		renderer.context.renderTargetEnd();

		texture.setTexture('prefilterMap', this.prefilterMap);

	}

	bufferData: { vertexArrayObject: WebGLVertexArrayObject, vertexBuffer: WebGLBuffer };
	captureViews: mat4[];
	irradianceMap: Texture;
	prefilterMap: Texture;

}
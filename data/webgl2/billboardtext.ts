import { Texture } from "./texture";
import { Overlay } from "./overlay/overlay";
import { Renderer } from "./glrenderer";
import { RenderTargetState } from "./rendertarget";
import { Sprite } from "./overlay/sprite";
import { vec2 } from "gl-matrix";
import { GeometryGenerator } from "./geometrygenerator";
import * as mesh from "./meshmanager";
import { StaticMesh } from "./mesh";
import * as material from './material';
import { ShaderType } from "./shader";

export class BillboardText {
    constructor(renderer: Renderer, overlay: Overlay) {
        this.overlay = overlay;
        this.gl = renderer.gl;
        this.renderer = renderer;
    }

    setText(text: string, letterHeight: number) {
        this.text = text;

        const width = 20 + 47 * text.length;
        const height = 64;
        this.renderTexture = Texture.createRenderTarget(this.gl, this.gl.RGBA, this.gl.RGBA, width, height, 
            this.gl.UNSIGNED_BYTE, this.gl.LINEAR, false, this.gl.LINEAR, true);

        const gl = this.gl;
        const context = this.renderer.getContext();
        const rts = new RenderTargetState(gl, { x: 0, y: 0, width, height });
        rts.addColorTarget(gl, 0, this.renderTexture);
        context.renderTargetBegin(rts); 
        gl.clearColor(0, 0.0, 0.0, 0.0);
        gl.clear(gl.COLOR_BUFFER_BIT);
        const viewport = context.screenViewPort;
        context.setViewport(rts.viewport);

        this.overlay.camera.setProjection(0.0, rts.viewport.width, 0,  rts.viewport.height, -1.0, 1.0);
        gl.depthFunc(gl.ALWAYS);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
        
        for(let index = 0; index < text.length; index++) {
            
            if(text.charAt(index) === ' ') {
                continue;
            }

            const subtexture = this.overlay.textureAtlas.subtextures[`images/${text.charAt(index).toUpperCase()}.png`];
            const sprite = new Sprite('temporary', subtexture);
            sprite.setAnchor(1, 0.5);
            sprite.setPosition(vec2.fromValues(20 + 47 * (index + 1), 32));
            sprite.setScale([1, 1]);
            this.overlay.renderSingleSprite(gl, sprite); 
        }

        context.renderTargetEnd();
        gl.depthFunc(gl.LESS);
        context.setViewport(viewport);
        this.overlay.camera.setProjection(0.0, window.innerWidth, window.innerHeight, 0, -1.0, 1.0);

        const aspectRatio = width / height;
        GeometryGenerator.GeneratePlane(gl, 'billboard-text', aspectRatio * letterHeight, letterHeight);
        this.billboardMesh = mesh.GetMesh('billboard-text');

        const mat: material.Material = {
            name: 'billboard-text-material',
            shader: ShaderType.BILLBOARD_PLAIN,
            tech: 'A',
            textures: [
                this.renderTexture
            ]
        };

        material.setMaterial(mat.name, mat);
        this.billboardMesh.getSubmesh('plane').materialID = mat.name;
    }

    billboardMesh: StaticMesh;
    renderTexture: Texture;
    material: string;

    text: string;
    overlay: Overlay;
    gl: WebGL2RenderingContext;
    renderer: Renderer;
}
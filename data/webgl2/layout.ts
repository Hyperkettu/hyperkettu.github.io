import { Overlay } from "../overlay";
import { Text } from './text';
import { vec2 } from "gl-matrix";
import { Container } from "../container";
import { Renderer } from "../../glrenderer";
import { Button } from "./button";
import { Element } from "./element";
import * as resource from "../../resource";
import * as textelement from './text';
import { Sprite } from "../sprite";
import { UISprite } from "./sprite";
import { AnimationData, Animation } from "../animationsystem";
import { Grid } from "./grid";
import * as ui from './container';
import { Slider } from "./slider";
import * as texture from '../../texturemanager';
import { Subtexture } from "../../subtexture";

const layouts: { [name: string]: UILayout } = {};

export function get(name: string) {
    return layouts[name];
}

export type ElementDataType = 'button' | 'text' | 'sprite' | 'grid' | 'container' | 'slider';
export type ElementType = Element | Button | Text | ui.Container | Slider;
export type ClickHandler = (x: number, y: number) => boolean;
export type DragHandler = (x: number, y: number) => number;

export interface LayoutFile {
    logicalSize: vec2;
    atlasFile: string;
    masks?: string[];
    separateImages?: string[];
    elements?: ElementData[];
    actions: AnimationData[];
    events: { [ name: string ]: EventData };
}

export interface ElementData {
    name: string;
    position: vec2;
    rotation: number;
    scale: vec2;
    anchor?: vec2;
    type: ElementDataType;
    children?: ElementData[];
    contentSize?: vec2;
}

export interface ButtonData extends ElementData {
    textData: TextData;
    spriteData: SpriteData;
}

export interface SliderData extends ElementData {
    backgroundData: SpriteData;
    holdData: SpriteData;
}

export interface TextData extends ElementData {
    text: string;
    atlasText?: AtlasTextData;
    fontDef?: FontDef;
}

export interface FontDef {
    fontSize: number;
    family: string;
    fillStyle: string; 
    textAlign: CanvasTextAlign;
    textBaseLine: CanvasTextBaseline; 
}

export interface SpriteData extends ElementData {
    path: string;
    separateImage?: boolean;
    maskPath?: string;
}

export interface GridData extends ElementData {

}

export interface ContainerData extends ElementData {}

export interface AtlasTextData {
    letterWidth: number;
    letterHeight: number;
    letterStyle: 'normal' | 'tilted';
    lineWidth: number;
    textAppearAnimation: textelement.TextAnimation;
}

export interface EventData {
    name: string; 
    actions: string[];
}

export class UILayout {
    constructor(renderer: Renderer, overlay: Overlay, logicalSize: vec2) {
        this.renderer = renderer;
        this.overlay = overlay;
        this.logicalSize = logicalSize;
        this.children = [];
        this.root = new Container('root');

        this.clickHandlers = [];
        this.releaseClickHandlers = [];
        this.dragHandlers = [];

        this.size = vec2.create();
        const size = vec2.fromValues(window.innerWidth, window.innerHeight);
        this.resize(size);

    }

    async runAnimation(elementName: string, animationName: string, settings: { instant: boolean }) {
        let animationData: AnimationData = null;
        for(let animation of this.animations) {
            if(elementName === animation.elementName && animationName === animation.animationName) {
                animationData = animation;
            }
        }
 
        const promise =  new Promise<void>(resolve => {
            const animation = this.createAnimation(animationData);
            animation.setEndCallback(() => {
                resolve();
            });
            this.overlay.startAnimation([animation], settings.instant);
        });
        await promise;
    }

    async event(name: string, settings: { instant: boolean } = { instant: false }) {

        const event = this.events[name];
        const actions: string[] = [];

        for(let action of event.actions){
            actions.push(action);
        }

        while(actions.length > 0) {

            const action = actions.shift();
            const animationDatas: AnimationData[] = [];
            for(let data of this.animations) {
                if(data.animationName === action) {
                    animationDatas.push(data);
                }
            }

            const promises: Promise<void>[] = [];

            for(let animationData of animationDatas) {
                const promise =  new Promise<void>(resolve => {
                    const animation = this.createAnimation(animationData);
                    animation.setEndCallback(() => {
                        resolve();
                    });
                    this.overlay.startAnimation([animation], settings?.instant);
                    if(settings?.instant) {
                        resolve();
                    }
                });
                promises.push(promise);
            }
            await Promise.all(promises);
        }
        
    }

    createAnimation(data: AnimationData) {
        const animation = new Animation(data.animationName, this.find(data.elementName).container, 
        data.target, data.easing, data.duration, data.type, data.delay);
        animation.setFrom(data.from);
        animation.setTo(data.to);
        return animation;
    }

    static async loadLayouts(renderer: Renderer, filePaths: string[]) {
        const promises: Promise<void>[] = [];
        for(let filePath of filePaths) {
            promises.push(UILayout.loadFromFile(renderer, filePath));
        }

        await Promise.all(promises);
    }

    static async loadFromFile(renderer: Renderer, fileName: string) {
        const file: LayoutFile = await resource.loadFile<LayoutFile>(fileName);
        const layout = new UILayout(renderer, renderer.overlay, file.logicalSize);
        await renderer.overlay.textureAtlas.loadFromJson(renderer.gl, file.atlasFile, renderer);

        const imagePromises: Promise<void>[] = [];

        if(file.masks) {
            for(let maskPath of file.masks) {
                imagePromises.push(texture.LoadTexture(renderer.gl, maskPath));
            }
        }

        if(file.separateImages) {
            for(let filePath of file.separateImages) {
                imagePromises.push(texture.LoadTexture(renderer.gl, filePath));
            }
        }

        await Promise.all(imagePromises);

        layout.events = file.events;
        layout.animations = file.actions;

        for(let elementData of file.elements) {
            const element = layout.createElement(elementData);

            layout.addElement(element);
        }

        layouts[fileName] = layout;
    }

    createElement(elementData: ElementData) {
        let element: ElementType = null;

        switch(elementData.type) {
            case 'button':
                const data = elementData as ButtonData;
                const sprite = this.createSprite(data.spriteData);
                const text = this.createText(data.textData);
                element = this.createButton(data, sprite, text);
                break;
            case 'text':
                element = this.createText(elementData as TextData);
                break;
            case 'sprite':
                 element = this.createUISprite(elementData as SpriteData);
                 break;
            case 'grid':
                 element = this.createGrid(elementData as GridData);
                 break;
           case 'container': 
                 element = this.createContainer(elementData as ContainerData);
                 break;
            case 'slider':
                const sliderData = elementData as SliderData;
                const background = this.createSprite(sliderData.backgroundData);
                const hold = this.createSprite(sliderData.holdData);
                element = this.createSlider(sliderData, background, hold);
            default: 
                break;
        }
        return element;
    }

    toJson() {

        const fileData: LayoutFile = { 
            logicalSize: this.logicalSize,
            atlasFile: 'images/atlas.json',
            actions: this.animations,
            events: this.events
         };

         fileData.elements = [];

        for(let element of this.children) {
            fileData.elements.push(element.toJson());
        }

        return JSON.stringify(fileData);
    }

    createContainer(data: ContainerData) {
        const container = new ui.Container(data.name, this.overlay, this);
        container.setPosition(data.position);
        container.setScale(data.scale);
        container.setRotation(data.rotation);

        if(data.children) {
            for(let child of data.children) {
                const element = this.createElement(child);
                container.addChild(element);
            }
        }

        if(data.anchor) {
            container.setAnchor(data.anchor);
        }

        return container;
    }

    createButton(data: ButtonData, sprite: Sprite, text: Text) {
        const button = new Button(data.name, this.overlay, sprite, text, this);
        button.setPosition(data.position);
        button.setScale(data.scale);
        button.setRotation(data.rotation);
        return button;
    }

    createSlider(data: SliderData, background: Sprite, hold: Sprite) {
        const slider = new Slider(data.name, this.overlay, background, hold, this);
        slider.setPosition(data.position);
        slider.setScale(data.scale);
        slider.setRotation(data.rotation);
        if(data.anchor) {
            slider.setAnchor(data.anchor);
        }
        return slider;
    }

    createSprite(data: SpriteData) {
        let sprite: Sprite = null;
        if(false) {//if(data.separateImage) {
            const tex = texture.GetTexture(data.path);
            const subtexture = new Subtexture(data.path, tex, 0, 0, tex.width, tex.height); 
            sprite = new Sprite(data.name, subtexture);
        } else {
            sprite = new Sprite(data.name, this.overlay.textureAtlas.subtextures[data.path]);
        }
        if(data.maskPath) {
            sprite.mask = texture.GetTexture(data.maskPath);
        }
        sprite.setPosition(data.position);
        sprite.setScale(data.scale);
        sprite.setAngle(data.rotation);
        if(data.anchor) {
            sprite.setAnchor(data.anchor[0], data.anchor[1]);
        }
        return sprite;
    }

    createUISprite(data: SpriteData) {
        let sprite: UISprite = null;
        sprite = new UISprite(data.name, this.overlay, this, { 
            path: data.path,
            separate: data.separateImage
        });

        sprite.setPosition(data.position);
        sprite.setScale(data.scale);
        sprite.setRotation(data.rotation);
        if(data.anchor) {
            sprite.setAnchor(data.anchor);
        }
        if(data.maskPath) {
            sprite.addMask(texture.GetTexture(data.maskPath));
        }
        return sprite;
    }

    createText(data: TextData) {
        
        const text = new Text(data.name, this.overlay, this, { 
			atlas: this.overlay.textureAtlas,
			gapInPixels: data.atlasText?.letterWidth, 
			style: data.atlasText?.letterStyle,
			lineHeight: data.atlasText?.letterHeight,
			lineWidth: data.atlasText?.lineWidth,
            textAppearAnimation: data.atlasText?.textAppearAnimation
         },
         {
            family: data.fontDef?.family,
            fillStyle: data.fontDef?.fillStyle,
            fontSize: data.fontDef?.fontSize,
            textAlign: data.fontDef?.textAlign,
            textBaseLine: data.fontDef?.textBaseLine
         });
         
         text.setText(data.text, this.renderer.gl);
         text.setPosition(data.position);
         text.setRotation(data.rotation);
		 text.setScale(data.scale);
    
         if(data.anchor) {
            text.container.setAnchor(data.anchor[0], data.anchor[1]);
        }
         
         return text;
    } 

    createGrid(data: GridData) {
        const grid = new Grid(data.name, this.overlay, this);
        grid.setPosition(data.position);
        grid.setRotation(data.rotation);
        grid.setScale(data.scale);
        grid.setContentSize(data.contentSize);
        if(data.anchor) {
            grid.setAnchor(data.anchor);
        }
        return grid;
    }

    addElement<T extends Element>(element: T) {
        this.root.addChild(element.container);
        this.children.push(element);
    }
    
    find<T extends Element>(name: string) {

        if(name === 'root') {
            const element = new Element('root', this.overlay, null);
            element.container = this.root;
            return element;
        }

        for(let child of this.children) {
            if(child.name === name) {
                return child;
            }
            const element = child.find<T>(name);

            if(element) {
                return element as (T extends Element ? T : Element);
            }
        }

        return undefined;
    }

    mapToScreen(vector: vec2, size: vec2) {
        const screenSize = vec2.create();
        const scale = Math.min(size[0] / this.logicalSize[0], size[1] / this.logicalSize[1]);
        vec2.scale(screenSize, vector, scale);
        return screenSize;
    }

    mapToLogical(vector: vec2, size: vec2) {
        const scale = this.mapToScreen(vec2.fromValues(1,1), size);
        const logical = vec2.create();
        vec2.scale(logical, vector, 1 / scale[0]);
        return logical;
    }

    resize(size: vec2) {
        if(this.size[0] !== size[0] || this.size[1] !== size[1]) {
            this.size = size;
            const scale = this.mapToScreen(vec2.fromValues(1,1), this.size);
            this.root.setScale(scale);

            for(let child of this.children) {
                
                const position = this.mapToLogical(child.position, this.size);
                child.container.setPosition(position);
            }
        }
    }

    renderer: Renderer;

    clickHandlers: ClickHandler[];
    releaseClickHandlers: ClickHandler[];
    dragHandlers: DragHandler[];

    size: vec2;

    children: Element[];

    root: Container;
    logicalSize: vec2;
    overlay: Overlay;

    animations: AnimationData[];
    events: { [name: string]: EventData };
}
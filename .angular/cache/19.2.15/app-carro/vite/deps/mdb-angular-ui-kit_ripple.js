import {
  coerceBooleanProperty
} from "./chunk-AGZFHLKA.js";
import "./chunk-JJBNIDOL.js";
import {
  Directive,
  ElementRef,
  HostBinding,
  HostListener,
  Input,
  NgModule,
  Renderer2,
  setClassMetadata,
  ɵɵclassProp,
  ɵɵdefineDirective,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵdirectiveInject,
  ɵɵlistener
} from "./chunk-RFZ2BTTM.js";
import "./chunk-WPM5VTLQ.js";
import "./chunk-PEBH6BBU.js";
import "./chunk-4S3KYZTJ.js";
import "./chunk-WDMUDEB6.js";

// node_modules/mdb-angular-ui-kit/fesm2022/mdb-angular-ui-kit-ripple.mjs
var DEFAULT_RIPPLE_COLOR = [0, 0, 0];
function durationToMsNumber(time) {
  return Number(time.replace("ms", "").replace("s", "000"));
}
function colorToRGB(color) {
  function hexToRgb(color2) {
    const HEX_COLOR_LENGTH = 7;
    const IS_SHORT_HEX = color2.length < HEX_COLOR_LENGTH;
    if (IS_SHORT_HEX) {
      color2 = `#${color2[1]}${color2[1]}${color2[2]}${color2[2]}${color2[3]}${color2[3]}`;
    }
    return [parseInt(color2.substr(1, 2), 16), parseInt(color2.substr(3, 2), 16), parseInt(color2.substr(5, 2), 16)];
  }
  function namedColorsToRgba(color2) {
    const tempElem = document.body.appendChild(document.createElement("fictum"));
    const flag = "rgb(1, 2, 3)";
    tempElem.style.color = flag;
    if (tempElem.style.color !== flag) {
      return DEFAULT_RIPPLE_COLOR;
    }
    tempElem.style.color = color2;
    if (tempElem.style.color === flag || tempElem.style.color === "") {
      return DEFAULT_RIPPLE_COLOR;
    }
    color2 = getComputedStyle(tempElem).color;
    document.body.removeChild(tempElem);
    return color2;
  }
  function rgbaToRgb(color2) {
    color2 = color2.match(/[.\d]+/g).map((a) => +Number(a));
    color2.length = 3;
    return color2;
  }
  if (color.toLowerCase() === "transparent") {
    return DEFAULT_RIPPLE_COLOR;
  }
  if (color[0] === "#") {
    return hexToRgb(color);
  }
  if (color.indexOf("rgb") === -1) {
    color = namedColorsToRgba(color);
  }
  if (color.indexOf("rgb") === 0) {
    return rgbaToRgb(color);
  }
  return DEFAULT_RIPPLE_COLOR;
}
function getDiameter({
  offsetX,
  offsetY,
  height,
  width
}) {
  const top = offsetY <= height / 2;
  const left = offsetX <= width / 2;
  const pythagorean = (sideA, sideB) => Math.sqrt(sideA ** 2 + sideB ** 2);
  const positionCenter = offsetY === height / 2 && offsetX === width / 2;
  const quadrant = {
    first: top === true && left === false,
    second: top === true && left === true,
    third: top === false && left === true,
    fourth: top === false && left === false
  };
  const getCorner = {
    topLeft: pythagorean(offsetX, offsetY),
    topRight: pythagorean(width - offsetX, offsetY),
    bottomLeft: pythagorean(offsetX, height - offsetY),
    bottomRight: pythagorean(width - offsetX, height - offsetY)
  };
  let diameter = 0;
  if (positionCenter || quadrant.fourth) {
    diameter = getCorner.topLeft;
  } else if (quadrant.third) {
    diameter = getCorner.topRight;
  } else if (quadrant.second) {
    diameter = getCorner.bottomRight;
  } else if (quadrant.first) {
    diameter = getCorner.bottomLeft;
  }
  return diameter * 2;
}
var TRANSITION_BREAK_OPACITY = 0.5;
var GRADIENT = "rgba({{color}}, 0.2) 0, rgba({{color}}, 0.3) 40%, rgba({{color}}, 0.4) 50%, rgba({{color}}, 0.5) 60%, rgba({{color}}, 0) 70%";
var BOOTSTRAP_COLORS = ["primary", "secondary", "success", "danger", "warning", "info", "light", "dark"];
var MdbRippleDirective = class _MdbRippleDirective {
  _elementRef;
  _renderer;
  get rippleCentered() {
    return this._rippleCentered;
  }
  set rippleCentered(value) {
    this._rippleCentered = coerceBooleanProperty(value);
  }
  _rippleCentered = false;
  rippleColor = "";
  rippleDuration = "500ms";
  rippleRadius = 0;
  get rippleUnbound() {
    return this._rippleUnbound;
  }
  set rippleUnbound(value) {
    this._rippleUnbound = coerceBooleanProperty(value);
  }
  _rippleUnbound = false;
  _rippleInSpan = false;
  _rippleTimer = null;
  constructor(_elementRef, _renderer) {
    this._elementRef = _elementRef;
    this._renderer = _renderer;
  }
  get host() {
    return this._elementRef.nativeElement;
  }
  ripple = true;
  _createRipple(event) {
    const {
      layerX,
      layerY
    } = event;
    const offsetX = layerX;
    const offsetY = layerY;
    const height = this.host.offsetHeight;
    const width = this.host.offsetWidth;
    const duration = durationToMsNumber(this.rippleDuration);
    const diameterOptions = {
      offsetX: this.rippleCentered ? height / 2 : offsetX,
      offsetY: this.rippleCentered ? width / 2 : offsetY,
      height,
      width
    };
    const diameter = getDiameter(diameterOptions);
    const radiusValue = this.rippleRadius || diameter / 2;
    const opacity = {
      delay: duration * TRANSITION_BREAK_OPACITY,
      duration: duration - duration * TRANSITION_BREAK_OPACITY
    };
    const styles = {
      left: this.rippleCentered ? `${width / 2 - radiusValue}px` : `${offsetX - radiusValue}px`,
      top: this.rippleCentered ? `${height / 2 - radiusValue}px` : `${offsetY - radiusValue}px`,
      height: `${this.rippleRadius * 2 || diameter}px`,
      width: `${this.rippleRadius * 2 || diameter}px`,
      transitionDelay: `0s, ${opacity.delay}ms`,
      transitionDuration: `${duration}ms, ${opacity.duration}ms`
    };
    const rippleHTML = this._renderer.createElement("div");
    if (this.host.tagName.toLowerCase() === "input") {
      this._createWrapperSpan();
    }
    this._createHTMLRipple(this.host, rippleHTML, styles);
    this._removeHTMLRipple(rippleHTML, duration);
  }
  _createWrapperSpan() {
    const parent = this._renderer.parentNode(this.host);
    this._rippleInSpan = true;
    if (parent.tagName.toLowerCase() === "span" && parent.classList.contains("ripple-surface")) {
      this._elementRef.nativeElement = parent;
    } else {
      const wrapper = this._renderer.createElement("span");
      this._renderer.addClass(wrapper, "ripple-surface");
      this._renderer.addClass(wrapper, "input-wrapper");
      this._renderer.setStyle(wrapper, "border", 0);
      const shadow = getComputedStyle(this.host).boxShadow;
      this._renderer.setStyle(wrapper, "box-shadow", shadow);
      parent.replaceChild(wrapper, this.host);
      wrapper.appendChild(this.host);
      this._elementRef.nativeElement = wrapper;
    }
    this.host.focus();
  }
  _removeWrapperSpan() {
    const child = this.host.firstChild;
    this.host.replaceWith(child);
    this._elementRef.nativeElement = child;
    this.host.focus();
    this._rippleInSpan = false;
  }
  _createHTMLRipple(wrapper, ripple, styles) {
    Object.keys(styles).forEach((property) => ripple.style[property] = styles[property]);
    this._renderer.addClass(ripple, "ripple-wave");
    if (this.rippleColor !== "") {
      this._removeOldColorClasses(wrapper);
      this._addColor(ripple, wrapper);
    }
    this._toggleUnbound(wrapper);
    this._appendRipple(ripple, wrapper);
  }
  _removeHTMLRipple(ripple, duration) {
    if (this._rippleTimer) {
      clearTimeout(this._rippleTimer);
      this._rippleTimer = null;
    }
    this._rippleTimer = setTimeout(() => {
      if (ripple) {
        ripple.remove();
        this.host.querySelectorAll(".ripple-wave").forEach((rippleEl) => {
          rippleEl.remove();
        });
        if (this._rippleInSpan && this.host.classList.contains("input-wrapper")) {
          this._removeWrapperSpan();
        }
      }
    }, duration);
  }
  _appendRipple(target, parent) {
    const FIX_ADD_RIPPLE_EFFECT = 50;
    this._renderer.appendChild(parent, target);
    setTimeout(() => {
      this._renderer.addClass(target, "active");
    }, FIX_ADD_RIPPLE_EFFECT);
  }
  _toggleUnbound(target) {
    if (this.rippleUnbound) {
      this._renderer.addClass(target, "ripple-surface-unbound");
    } else {
      this._renderer.removeClass(target, "ripple-surface-unbound");
    }
  }
  _addColor(target, parent) {
    const isBootstrapColor = BOOTSTRAP_COLORS.find((color) => color === this.rippleColor.toLowerCase());
    if (isBootstrapColor) {
      this._renderer.addClass(parent, `${"ripple-surface"}-${this.rippleColor.toLowerCase()}`);
    } else {
      const rgbValue = colorToRGB(this.rippleColor).join(",");
      const gradientImage = GRADIENT.split("{{color}}").join(`${rgbValue}`);
      target.style.backgroundImage = `radial-gradient(circle, ${gradientImage})`;
    }
  }
  _removeOldColorClasses(target) {
    const REGEXP_CLASS_COLOR = new RegExp(`${"ripple-surface"}-[a-z]+`, "gi");
    const PARENT_CLASSS_COLOR = target.classList.value.match(REGEXP_CLASS_COLOR) || [];
    PARENT_CLASSS_COLOR.forEach((className) => {
      this._renderer.removeClass(target, className);
    });
  }
  static ngAcceptInputType_rippleCentered;
  static ngAcceptInputType_rippleUnbound;
  static ɵfac = function MdbRippleDirective_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MdbRippleDirective)(ɵɵdirectiveInject(ElementRef), ɵɵdirectiveInject(Renderer2));
  };
  static ɵdir = ɵɵdefineDirective({
    type: _MdbRippleDirective,
    selectors: [["", "mdbRipple", ""]],
    hostVars: 2,
    hostBindings: function MdbRippleDirective_HostBindings(rf, ctx) {
      if (rf & 1) {
        ɵɵlistener("click", function MdbRippleDirective_click_HostBindingHandler($event) {
          return ctx._createRipple($event);
        });
      }
      if (rf & 2) {
        ɵɵclassProp("ripple-surface", ctx.ripple);
      }
    },
    inputs: {
      rippleCentered: "rippleCentered",
      rippleColor: "rippleColor",
      rippleDuration: "rippleDuration",
      rippleRadius: "rippleRadius",
      rippleUnbound: "rippleUnbound"
    },
    exportAs: ["mdbRipple"],
    standalone: false
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MdbRippleDirective, [{
    type: Directive,
    args: [{
      // eslint-disable-next-line @angular-eslint/directive-selector
      selector: "[mdbRipple]",
      exportAs: "mdbRipple",
      standalone: false
    }]
  }], () => [{
    type: ElementRef
  }, {
    type: Renderer2
  }], {
    rippleCentered: [{
      type: Input
    }],
    rippleColor: [{
      type: Input
    }],
    rippleDuration: [{
      type: Input
    }],
    rippleRadius: [{
      type: Input
    }],
    rippleUnbound: [{
      type: Input
    }],
    ripple: [{
      type: HostBinding,
      args: ["class.ripple-surface"]
    }],
    _createRipple: [{
      type: HostListener,
      args: ["click", ["$event"]]
    }]
  });
})();
var MdbRippleModule = class _MdbRippleModule {
  static ɵfac = function MdbRippleModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MdbRippleModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _MdbRippleModule,
    declarations: [MdbRippleDirective],
    exports: [MdbRippleDirective]
  });
  static ɵinj = ɵɵdefineInjector({});
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MdbRippleModule, [{
    type: NgModule,
    args: [{
      declarations: [MdbRippleDirective],
      imports: [],
      exports: [MdbRippleDirective]
    }]
  }], null, null);
})();
export {
  MdbRippleDirective,
  MdbRippleModule
};
//# sourceMappingURL=mdb-angular-ui-kit_ripple.js.map

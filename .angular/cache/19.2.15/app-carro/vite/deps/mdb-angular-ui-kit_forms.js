import {
  ContentObserver,
  Platform,
  _CdkPrivateStyleLoader,
  _bindEventWithOptions
} from "./chunk-4TMFXSLD.js";
import {
  FormsModule,
  NgControl
} from "./chunk-ZE567BST.js";
import {
  CommonModule
} from "./chunk-O4HOMCSG.js";
import {
  DOCUMENT
} from "./chunk-BPZ7AUX4.js";
import {
  coerceBooleanProperty
} from "./chunk-AGZFHLKA.js";
import {
  coerceElement,
  coerceNumberProperty
} from "./chunk-UZRDOEGV.js";
import {
  ChangeDetectionStrategy,
  Component,
  ContentChild,
  DestroyRef,
  Directive,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Injectable,
  Input,
  NgModule,
  NgZone,
  Optional,
  Output,
  Renderer2,
  RendererFactory2,
  Self,
  ViewChild,
  ViewEncapsulation,
  assertInInjectionContext,
  booleanAttribute,
  inject,
  setClassMetadata,
  ɵɵProvidersFeature,
  ɵɵclassProp,
  ɵɵcontentQuery,
  ɵɵdefineComponent,
  ɵɵdefineDirective,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵdirectiveInject,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵhostProperty,
  ɵɵlistener,
  ɵɵloadQuery,
  ɵɵprojection,
  ɵɵprojectionDef,
  ɵɵqueryRefresh,
  ɵɵviewQuery
} from "./chunk-6SZJNWHU.js";
import "./chunk-FFZIAYYX.js";
import "./chunk-6Q4RANH6.js";
import {
  EMPTY,
  Observable,
  Subject,
  auditTime,
  takeUntil
} from "./chunk-CXCX2JKZ.js";

// node_modules/@angular/core/fesm2022/rxjs-interop.mjs
function takeUntilDestroyed(destroyRef) {
  if (!destroyRef) {
    assertInInjectionContext(takeUntilDestroyed);
    destroyRef = inject(DestroyRef);
  }
  const destroyed$ = new Observable((observer) => {
    const unregisterFn = destroyRef.onDestroy(observer.next.bind(observer));
    return unregisterFn;
  });
  return (source) => {
    return source.pipe(takeUntil(destroyed$));
  };
}

// node_modules/@angular/cdk/fesm2022/text-field.mjs
var _CdkTextFieldStyleLoader = class __CdkTextFieldStyleLoader {
  static ɵfac = function _CdkTextFieldStyleLoader_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || __CdkTextFieldStyleLoader)();
  };
  static ɵcmp = ɵɵdefineComponent({
    type: __CdkTextFieldStyleLoader,
    selectors: [["ng-component"]],
    hostAttrs: ["cdk-text-field-style-loader", ""],
    decls: 0,
    vars: 0,
    template: function _CdkTextFieldStyleLoader_Template(rf, ctx) {
    },
    styles: ["textarea.cdk-textarea-autosize{resize:none}textarea.cdk-textarea-autosize-measuring{padding:2px 0 !important;box-sizing:content-box !important;height:auto !important;overflow:hidden !important}textarea.cdk-textarea-autosize-measuring-firefox{padding:2px 0 !important;box-sizing:content-box !important;height:0 !important}@keyframes cdk-text-field-autofill-start{/*!*/}@keyframes cdk-text-field-autofill-end{/*!*/}.cdk-text-field-autofill-monitored:-webkit-autofill{animation:cdk-text-field-autofill-start 0s 1ms}.cdk-text-field-autofill-monitored:not(:-webkit-autofill){animation:cdk-text-field-autofill-end 0s 1ms}\n"],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(_CdkTextFieldStyleLoader, [{
    type: Component,
    args: [{
      template: "",
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      host: {
        "cdk-text-field-style-loader": ""
      },
      styles: ["textarea.cdk-textarea-autosize{resize:none}textarea.cdk-textarea-autosize-measuring{padding:2px 0 !important;box-sizing:content-box !important;height:auto !important;overflow:hidden !important}textarea.cdk-textarea-autosize-measuring-firefox{padding:2px 0 !important;box-sizing:content-box !important;height:0 !important}@keyframes cdk-text-field-autofill-start{/*!*/}@keyframes cdk-text-field-autofill-end{/*!*/}.cdk-text-field-autofill-monitored:-webkit-autofill{animation:cdk-text-field-autofill-start 0s 1ms}.cdk-text-field-autofill-monitored:not(:-webkit-autofill){animation:cdk-text-field-autofill-end 0s 1ms}\n"]
    }]
  }], null, null);
})();
var listenerOptions = {
  passive: true
};
var AutofillMonitor = class _AutofillMonitor {
  _platform = inject(Platform);
  _ngZone = inject(NgZone);
  _renderer = inject(RendererFactory2).createRenderer(null, null);
  _styleLoader = inject(_CdkPrivateStyleLoader);
  _monitoredElements = /* @__PURE__ */ new Map();
  constructor() {
  }
  monitor(elementOrRef) {
    if (!this._platform.isBrowser) {
      return EMPTY;
    }
    this._styleLoader.load(_CdkTextFieldStyleLoader);
    const element = coerceElement(elementOrRef);
    const info = this._monitoredElements.get(element);
    if (info) {
      return info.subject;
    }
    const subject = new Subject();
    const cssClass = "cdk-text-field-autofilled";
    const listener = (event) => {
      if (event.animationName === "cdk-text-field-autofill-start" && !element.classList.contains(cssClass)) {
        element.classList.add(cssClass);
        this._ngZone.run(() => subject.next({
          target: event.target,
          isAutofilled: true
        }));
      } else if (event.animationName === "cdk-text-field-autofill-end" && element.classList.contains(cssClass)) {
        element.classList.remove(cssClass);
        this._ngZone.run(() => subject.next({
          target: event.target,
          isAutofilled: false
        }));
      }
    };
    const unlisten = this._ngZone.runOutsideAngular(() => {
      element.classList.add("cdk-text-field-autofill-monitored");
      return _bindEventWithOptions(this._renderer, element, "animationstart", listener, listenerOptions);
    });
    this._monitoredElements.set(element, {
      subject,
      unlisten
    });
    return subject;
  }
  stopMonitoring(elementOrRef) {
    const element = coerceElement(elementOrRef);
    const info = this._monitoredElements.get(element);
    if (info) {
      info.unlisten();
      info.subject.complete();
      element.classList.remove("cdk-text-field-autofill-monitored");
      element.classList.remove("cdk-text-field-autofilled");
      this._monitoredElements.delete(element);
    }
  }
  ngOnDestroy() {
    this._monitoredElements.forEach((_info, element) => this.stopMonitoring(element));
  }
  static ɵfac = function AutofillMonitor_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _AutofillMonitor)();
  };
  static ɵprov = ɵɵdefineInjectable({
    token: _AutofillMonitor,
    factory: _AutofillMonitor.ɵfac,
    providedIn: "root"
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(AutofillMonitor, [{
    type: Injectable,
    args: [{
      providedIn: "root"
    }]
  }], () => [], null);
})();
var CdkAutofill = class _CdkAutofill {
  _elementRef = inject(ElementRef);
  _autofillMonitor = inject(AutofillMonitor);
  /** Emits when the autofill state of the element changes. */
  cdkAutofill = new EventEmitter();
  constructor() {
  }
  ngOnInit() {
    this._autofillMonitor.monitor(this._elementRef).subscribe((event) => this.cdkAutofill.emit(event));
  }
  ngOnDestroy() {
    this._autofillMonitor.stopMonitoring(this._elementRef);
  }
  static ɵfac = function CdkAutofill_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _CdkAutofill)();
  };
  static ɵdir = ɵɵdefineDirective({
    type: _CdkAutofill,
    selectors: [["", "cdkAutofill", ""]],
    outputs: {
      cdkAutofill: "cdkAutofill"
    }
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(CdkAutofill, [{
    type: Directive,
    args: [{
      selector: "[cdkAutofill]"
    }]
  }], () => [], {
    cdkAutofill: [{
      type: Output
    }]
  });
})();
var CdkTextareaAutosize = class _CdkTextareaAutosize {
  _elementRef = inject(ElementRef);
  _platform = inject(Platform);
  _ngZone = inject(NgZone);
  _renderer = inject(Renderer2);
  _resizeEvents = new Subject();
  /** Keep track of the previous textarea value to avoid resizing when the value hasn't changed. */
  _previousValue;
  _initialHeight;
  _destroyed = new Subject();
  _listenerCleanups;
  _minRows;
  _maxRows;
  _enabled = true;
  /**
   * Value of minRows as of last resize. If the minRows has decreased, the
   * height of the textarea needs to be recomputed to reflect the new minimum. The maxHeight
   * does not have the same problem because it does not affect the textarea's scrollHeight.
   */
  _previousMinRows = -1;
  _textareaElement;
  /** Minimum amount of rows in the textarea. */
  get minRows() {
    return this._minRows;
  }
  set minRows(value) {
    this._minRows = coerceNumberProperty(value);
    this._setMinHeight();
  }
  /** Maximum amount of rows in the textarea. */
  get maxRows() {
    return this._maxRows;
  }
  set maxRows(value) {
    this._maxRows = coerceNumberProperty(value);
    this._setMaxHeight();
  }
  /** Whether autosizing is enabled or not */
  get enabled() {
    return this._enabled;
  }
  set enabled(value) {
    if (this._enabled !== value) {
      (this._enabled = value) ? this.resizeToFitContent(true) : this.reset();
    }
  }
  get placeholder() {
    return this._textareaElement.placeholder;
  }
  set placeholder(value) {
    this._cachedPlaceholderHeight = void 0;
    if (value) {
      this._textareaElement.setAttribute("placeholder", value);
    } else {
      this._textareaElement.removeAttribute("placeholder");
    }
    this._cacheTextareaPlaceholderHeight();
  }
  /** Cached height of a textarea with a single row. */
  _cachedLineHeight;
  /** Cached height of a textarea with only the placeholder. */
  _cachedPlaceholderHeight;
  /** Cached scroll top of a textarea */
  _cachedScrollTop;
  /** Used to reference correct document/window */
  _document = inject(DOCUMENT, {
    optional: true
  });
  _hasFocus;
  _isViewInited = false;
  constructor() {
    const styleLoader = inject(_CdkPrivateStyleLoader);
    styleLoader.load(_CdkTextFieldStyleLoader);
    this._textareaElement = this._elementRef.nativeElement;
  }
  /** Sets the minimum height of the textarea as determined by minRows. */
  _setMinHeight() {
    const minHeight = this.minRows && this._cachedLineHeight ? `${this.minRows * this._cachedLineHeight}px` : null;
    if (minHeight) {
      this._textareaElement.style.minHeight = minHeight;
    }
  }
  /** Sets the maximum height of the textarea as determined by maxRows. */
  _setMaxHeight() {
    const maxHeight = this.maxRows && this._cachedLineHeight ? `${this.maxRows * this._cachedLineHeight}px` : null;
    if (maxHeight) {
      this._textareaElement.style.maxHeight = maxHeight;
    }
  }
  ngAfterViewInit() {
    if (this._platform.isBrowser) {
      this._initialHeight = this._textareaElement.style.height;
      this.resizeToFitContent();
      this._ngZone.runOutsideAngular(() => {
        this._listenerCleanups = [this._renderer.listen("window", "resize", () => this._resizeEvents.next()), this._renderer.listen(this._textareaElement, "focus", this._handleFocusEvent), this._renderer.listen(this._textareaElement, "blur", this._handleFocusEvent)];
        this._resizeEvents.pipe(auditTime(16)).subscribe(() => {
          this._cachedLineHeight = this._cachedPlaceholderHeight = void 0;
          this.resizeToFitContent(true);
        });
      });
      this._isViewInited = true;
      this.resizeToFitContent(true);
    }
  }
  ngOnDestroy() {
    this._listenerCleanups?.forEach((cleanup) => cleanup());
    this._resizeEvents.complete();
    this._destroyed.next();
    this._destroyed.complete();
  }
  /**
   * Cache the height of a single-row textarea if it has not already been cached.
   *
   * We need to know how large a single "row" of a textarea is in order to apply minRows and
   * maxRows. For the initial version, we will assume that the height of a single line in the
   * textarea does not ever change.
   */
  _cacheTextareaLineHeight() {
    if (this._cachedLineHeight) {
      return;
    }
    const textareaClone = this._textareaElement.cloneNode(false);
    const cloneStyles = textareaClone.style;
    textareaClone.rows = 1;
    cloneStyles.position = "absolute";
    cloneStyles.visibility = "hidden";
    cloneStyles.border = "none";
    cloneStyles.padding = "0";
    cloneStyles.height = "";
    cloneStyles.minHeight = "";
    cloneStyles.maxHeight = "";
    cloneStyles.top = cloneStyles.bottom = cloneStyles.left = cloneStyles.right = "auto";
    cloneStyles.overflow = "hidden";
    this._textareaElement.parentNode.appendChild(textareaClone);
    this._cachedLineHeight = textareaClone.clientHeight;
    textareaClone.remove();
    this._setMinHeight();
    this._setMaxHeight();
  }
  _measureScrollHeight() {
    const element = this._textareaElement;
    const previousMargin = element.style.marginBottom || "";
    const isFirefox = this._platform.FIREFOX;
    const needsMarginFiller = isFirefox && this._hasFocus;
    const measuringClass = isFirefox ? "cdk-textarea-autosize-measuring-firefox" : "cdk-textarea-autosize-measuring";
    if (needsMarginFiller) {
      element.style.marginBottom = `${element.clientHeight}px`;
    }
    element.classList.add(measuringClass);
    const scrollHeight = element.scrollHeight - 4;
    element.classList.remove(measuringClass);
    if (needsMarginFiller) {
      element.style.marginBottom = previousMargin;
    }
    return scrollHeight;
  }
  _cacheTextareaPlaceholderHeight() {
    if (!this._isViewInited || this._cachedPlaceholderHeight != void 0) {
      return;
    }
    if (!this.placeholder) {
      this._cachedPlaceholderHeight = 0;
      return;
    }
    const value = this._textareaElement.value;
    this._textareaElement.value = this._textareaElement.placeholder;
    this._cachedPlaceholderHeight = this._measureScrollHeight();
    this._textareaElement.value = value;
  }
  /** Handles `focus` and `blur` events. */
  _handleFocusEvent = (event) => {
    this._hasFocus = event.type === "focus";
  };
  ngDoCheck() {
    if (this._platform.isBrowser) {
      this.resizeToFitContent();
    }
  }
  /**
   * Resize the textarea to fit its content.
   * @param force Whether to force a height recalculation. By default the height will be
   *    recalculated only if the value changed since the last call.
   */
  resizeToFitContent(force = false) {
    if (!this._enabled) {
      return;
    }
    this._cacheTextareaLineHeight();
    this._cacheTextareaPlaceholderHeight();
    this._cachedScrollTop = this._textareaElement.scrollTop;
    if (!this._cachedLineHeight) {
      return;
    }
    const textarea = this._elementRef.nativeElement;
    const value = textarea.value;
    if (!force && this._minRows === this._previousMinRows && value === this._previousValue) {
      return;
    }
    const scrollHeight = this._measureScrollHeight();
    const height = Math.max(scrollHeight, this._cachedPlaceholderHeight || 0);
    textarea.style.height = `${height}px`;
    this._ngZone.runOutsideAngular(() => {
      if (typeof requestAnimationFrame !== "undefined") {
        requestAnimationFrame(() => this._scrollToCaretPosition(textarea));
      } else {
        setTimeout(() => this._scrollToCaretPosition(textarea));
      }
    });
    this._previousValue = value;
    this._previousMinRows = this._minRows;
  }
  /**
   * Resets the textarea to its original size
   */
  reset() {
    if (this._initialHeight !== void 0) {
      this._textareaElement.style.height = this._initialHeight;
    }
  }
  _noopInputHandler() {
  }
  /**
   * Scrolls a textarea to the caret position. On Firefox resizing the textarea will
   * prevent it from scrolling to the caret position. We need to re-set the selection
   * in order for it to scroll to the proper position.
   */
  _scrollToCaretPosition(textarea) {
    const {
      selectionStart,
      selectionEnd
    } = textarea;
    if (!this._destroyed.isStopped && this._hasFocus) {
      textarea.setSelectionRange(selectionStart, selectionEnd);
      textarea.scrollTop = this._cachedScrollTop;
    }
  }
  static ɵfac = function CdkTextareaAutosize_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _CdkTextareaAutosize)();
  };
  static ɵdir = ɵɵdefineDirective({
    type: _CdkTextareaAutosize,
    selectors: [["textarea", "cdkTextareaAutosize", ""]],
    hostAttrs: ["rows", "1", 1, "cdk-textarea-autosize"],
    hostBindings: function CdkTextareaAutosize_HostBindings(rf, ctx) {
      if (rf & 1) {
        ɵɵlistener("input", function CdkTextareaAutosize_input_HostBindingHandler() {
          return ctx._noopInputHandler();
        });
      }
    },
    inputs: {
      minRows: [0, "cdkAutosizeMinRows", "minRows"],
      maxRows: [0, "cdkAutosizeMaxRows", "maxRows"],
      enabled: [2, "cdkTextareaAutosize", "enabled", booleanAttribute],
      placeholder: "placeholder"
    },
    exportAs: ["cdkTextareaAutosize"]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(CdkTextareaAutosize, [{
    type: Directive,
    args: [{
      selector: "textarea[cdkTextareaAutosize]",
      exportAs: "cdkTextareaAutosize",
      host: {
        "class": "cdk-textarea-autosize",
        // Textarea elements that have the directive applied should have a single row by default.
        // Browsers normally show two rows by default and therefore this limits the minRows binding.
        "rows": "1",
        "(input)": "_noopInputHandler()"
      }
    }]
  }], () => [], {
    minRows: [{
      type: Input,
      args: ["cdkAutosizeMinRows"]
    }],
    maxRows: [{
      type: Input,
      args: ["cdkAutosizeMaxRows"]
    }],
    enabled: [{
      type: Input,
      args: [{
        alias: "cdkTextareaAutosize",
        transform: booleanAttribute
      }]
    }],
    placeholder: [{
      type: Input
    }]
  });
})();
var TextFieldModule = class _TextFieldModule {
  static ɵfac = function TextFieldModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _TextFieldModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _TextFieldModule,
    imports: [CdkAutofill, CdkTextareaAutosize],
    exports: [CdkAutofill, CdkTextareaAutosize]
  });
  static ɵinj = ɵɵdefineInjector({});
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(TextFieldModule, [{
    type: NgModule,
    args: [{
      imports: [CdkAutofill, CdkTextareaAutosize],
      exports: [CdkAutofill, CdkTextareaAutosize]
    }]
  }], null, null);
})();

// node_modules/mdb-angular-ui-kit/fesm2022/mdb-angular-ui-kit-forms.mjs
var _c0 = ["notchLeading"];
var _c1 = ["notchMiddle"];
var _c2 = ["*"];
var MdbAbstractFormControl = class _MdbAbstractFormControl {
  stateChanges;
  input;
  labelActive;
  static ɵfac = function MdbAbstractFormControl_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MdbAbstractFormControl)();
  };
  static ɵdir = ɵɵdefineDirective({
    type: _MdbAbstractFormControl
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MdbAbstractFormControl, [{
    type: Directive
  }], null, null);
})();
var MdbLabelDirective = class _MdbLabelDirective {
  constructor() {
  }
  static ɵfac = function MdbLabelDirective_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MdbLabelDirective)();
  };
  static ɵdir = ɵɵdefineDirective({
    type: _MdbLabelDirective,
    selectors: [["", "mdbLabel", ""]],
    exportAs: ["mdbLabel"],
    standalone: false
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MdbLabelDirective, [{
    type: Directive,
    args: [{
      // eslint-disable-next-line @angular-eslint/directive-selector
      selector: "[mdbLabel]",
      exportAs: "mdbLabel",
      standalone: false
    }]
  }], () => [], null);
})();
var MdbFormControlComponent = class _MdbFormControlComponent {
  _renderer;
  _contentObserver;
  _elementRef;
  _ngZone;
  _notchLeading;
  _notchMiddle;
  _formControl;
  _label;
  outline = true;
  display = true;
  get input() {
    return this._formControl.input;
  }
  constructor(_renderer, _contentObserver, _elementRef, _ngZone) {
    this._renderer = _renderer;
    this._contentObserver = _contentObserver;
    this._elementRef = _elementRef;
    this._ngZone = _ngZone;
  }
  _destroy$ = new Subject();
  _notchLeadingLength = 9;
  _labelMarginLeft = 0;
  _labelGapPadding = 8;
  _labelScale = 0.8;
  _recalculateGapWhenVisible = false;
  ngAfterContentInit() {
    if (this._label) {
      setTimeout(() => {
        this._updateBorderGap();
      }, 0);
    } else {
      this._renderer.addClass(this.input, "placeholder-active");
    }
    this._updateLabelActiveState();
    if (this._label) {
      this._contentObserver.observe(this._label.nativeElement).pipe(takeUntil(this._destroy$)).subscribe(() => {
        this._updateBorderGap();
      });
    }
    this._formControl.stateChanges.pipe(takeUntil(this._destroy$)).subscribe(() => {
      this._updateLabelActiveState();
      if (this._label) {
        this._updateBorderGap();
      }
    });
    this._ngZone.runOutsideAngular(() => {
      this._ngZone.onStable.pipe(takeUntil(this._destroy$)).subscribe(() => {
        if (this._label && this._recalculateGapWhenVisible) {
          this._updateBorderGap();
        }
      });
    });
  }
  ngOnDestroy() {
    this._destroy$.next();
    this._destroy$.unsubscribe();
  }
  _getLabelWidth() {
    return this._label.nativeElement.clientWidth * this._labelScale + this._labelGapPadding;
  }
  _updateBorderGap() {
    if (this._isHidden()) {
      this._recalculateGapWhenVisible = true;
      return;
    }
    const notchLeadingWidth = `${this._labelMarginLeft + this._notchLeadingLength}px`;
    const notchMiddleWidth = `${this._getLabelWidth()}px`;
    this._notchLeading.nativeElement.style.width = notchLeadingWidth;
    this._notchMiddle.nativeElement.style.width = notchMiddleWidth;
    this._label.nativeElement.style.marginLeft = `${this._labelMarginLeft}px`;
    this._recalculateGapWhenVisible = false;
  }
  _updateLabelActiveState() {
    if (this._isLabelActive()) {
      this._renderer.addClass(this.input, "active");
    } else {
      this._renderer.removeClass(this.input, "active");
    }
  }
  _isLabelActive() {
    return this._formControl && this._formControl.labelActive;
  }
  _isHidden() {
    const el = this._elementRef.nativeElement;
    return !el.offsetHeight && !el.offsetWidth;
  }
  static ɵfac = function MdbFormControlComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MdbFormControlComponent)(ɵɵdirectiveInject(Renderer2), ɵɵdirectiveInject(ContentObserver), ɵɵdirectiveInject(ElementRef), ɵɵdirectiveInject(NgZone));
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _MdbFormControlComponent,
    selectors: [["mdb-form-control"]],
    contentQueries: function MdbFormControlComponent_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, MdbAbstractFormControl, 7);
        ɵɵcontentQuery(dirIndex, MdbLabelDirective, 7, ElementRef);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx._formControl = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx._label = _t.first);
      }
    },
    viewQuery: function MdbFormControlComponent_Query(rf, ctx) {
      if (rf & 1) {
        ɵɵviewQuery(_c0, 7);
        ɵɵviewQuery(_c1, 7);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx._notchLeading = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx._notchMiddle = _t.first);
      }
    },
    hostVars: 4,
    hostBindings: function MdbFormControlComponent_HostBindings(rf, ctx) {
      if (rf & 2) {
        ɵɵclassProp("form-outline", ctx.outline)("d-block", ctx.display);
      }
    },
    standalone: false,
    ngContentSelectors: _c2,
    decls: 7,
    vars: 0,
    consts: [["notchLeading", ""], ["notchMiddle", ""], [1, "form-notch"], [1, "form-notch-leading"], [1, "form-notch-middle"], [1, "form-notch-trailing"]],
    template: function MdbFormControlComponent_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵprojectionDef();
        ɵɵprojection(0);
        ɵɵelementStart(1, "div", 2);
        ɵɵelement(2, "div", 3, 0)(4, "div", 4, 1)(6, "div", 5);
        ɵɵelementEnd();
      }
    },
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MdbFormControlComponent, [{
    type: Component,
    args: [{
      selector: "mdb-form-control",
      changeDetection: ChangeDetectionStrategy.OnPush,
      standalone: false,
      template: '<ng-content></ng-content>\n<div class="form-notch">\n  <div #notchLeading class="form-notch-leading"></div>\n  <div #notchMiddle class="form-notch-middle"></div>\n  <div class="form-notch-trailing"></div>\n</div>\n'
    }]
  }], () => [{
    type: Renderer2
  }, {
    type: ContentObserver
  }, {
    type: ElementRef
  }, {
    type: NgZone
  }], {
    _notchLeading: [{
      type: ViewChild,
      args: ["notchLeading", {
        static: true
      }]
    }],
    _notchMiddle: [{
      type: ViewChild,
      args: ["notchMiddle", {
        static: true
      }]
    }],
    _formControl: [{
      type: ContentChild,
      args: [MdbAbstractFormControl, {
        static: true
      }]
    }],
    _label: [{
      type: ContentChild,
      args: [MdbLabelDirective, {
        static: true,
        read: ElementRef
      }]
    }],
    outline: [{
      type: HostBinding,
      args: ["class.form-outline"]
    }],
    display: [{
      type: HostBinding,
      args: ["class.d-block"]
    }]
  });
})();
var MdbInputDirective = class _MdbInputDirective {
  _elementRef;
  _renderer;
  _ngControl;
  _autofill;
  _destroyRef;
  constructor(_elementRef, _renderer, _ngControl, _autofill, _destroyRef) {
    this._elementRef = _elementRef;
    this._renderer = _renderer;
    this._ngControl = _ngControl;
    this._autofill = _autofill;
    this._destroyRef = _destroyRef;
  }
  stateChanges = new Subject();
  _focused = false;
  _autofilled = false;
  _color = "";
  ngAfterViewInit() {
    if (typeof getComputedStyle === "function") {
      this._color = getComputedStyle(this._elementRef.nativeElement).color;
      if (this._hasTypeInterferingPlaceholder()) {
        this._updateTextColorForDateType();
      }
    }
    this._autofill.monitor(this.input).subscribe((event) => {
      this._autofilled = event.isAutofilled;
      this.stateChanges.next();
    });
    this.stateChanges.pipe(takeUntilDestroyed(this._destroyRef)).subscribe(() => {
      if (this._hasTypeInterferingPlaceholder()) {
        this._updateTextColorForDateType();
      }
    });
  }
  _currentNativeValue;
  get disabled() {
    if (this._ngControl && this._ngControl.disabled !== null) {
      return this._ngControl.disabled;
    }
    return this._disabled;
  }
  set disabled(value) {
    this._disabled = coerceBooleanProperty(value);
  }
  _disabled = false;
  get readonly() {
    return this._readonly;
  }
  set readonly(value) {
    if (value) {
      this._renderer.setAttribute(this._elementRef.nativeElement, "readonly", "");
    } else {
      this._renderer.removeAttribute(this._elementRef.nativeElement, "readonly");
    }
    this._readonly = coerceBooleanProperty(value);
  }
  _readonly = false;
  get value() {
    return this._elementRef.nativeElement.value;
  }
  set value(value) {
    if (value !== this.value) {
      this._elementRef.nativeElement.value = value;
      this._value = value;
      this.stateChanges.next();
    }
  }
  _value;
  _updateTextColorForDateType() {
    const actualColor = getComputedStyle(this._elementRef.nativeElement).color;
    this._color = actualColor !== "rgba(0, 0, 0, 0)" ? actualColor : this._color;
    const color = this.labelActive ? this._color : `transparent`;
    this._renderer.setStyle(this._elementRef.nativeElement, "color", color);
  }
  _onFocus() {
    this._focused = true;
    this.stateChanges.next();
  }
  _onBlur() {
    this._focused = false;
    this.stateChanges.next();
  }
  ngDoCheck() {
    const value = this._elementRef.nativeElement.value;
    if (this._currentNativeValue !== value) {
      this._currentNativeValue = value;
      this.stateChanges.next();
    }
  }
  get hasValue() {
    return this._elementRef.nativeElement.value !== "";
  }
  get focused() {
    return this._focused;
  }
  get autofilled() {
    return this._autofilled;
  }
  get input() {
    return this._elementRef.nativeElement;
  }
  get labelActive() {
    return this.focused || this.hasValue || this.autofilled;
  }
  _hasTypeInterferingPlaceholder() {
    const typesArray = ["date", "datetime-local", "time", "month", "week"];
    return typesArray.includes(this._elementRef.nativeElement.type);
  }
  static ngAcceptInputType_disabled;
  static ngAcceptInputType_readonly;
  ngOnDestroy() {
    this._autofill.stopMonitoring(this.input);
  }
  static ɵfac = function MdbInputDirective_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MdbInputDirective)(ɵɵdirectiveInject(ElementRef), ɵɵdirectiveInject(Renderer2), ɵɵdirectiveInject(NgControl, 10), ɵɵdirectiveInject(AutofillMonitor), ɵɵdirectiveInject(DestroyRef));
  };
  static ɵdir = ɵɵdefineDirective({
    type: _MdbInputDirective,
    selectors: [["", "mdbInput", ""]],
    hostVars: 1,
    hostBindings: function MdbInputDirective_HostBindings(rf, ctx) {
      if (rf & 1) {
        ɵɵlistener("focus", function MdbInputDirective_focus_HostBindingHandler() {
          return ctx._onFocus();
        })("blur", function MdbInputDirective_blur_HostBindingHandler() {
          return ctx._onBlur();
        });
      }
      if (rf & 2) {
        ɵɵhostProperty("disabled", ctx.disabled);
      }
    },
    inputs: {
      disabled: "disabled",
      readonly: "readonly",
      value: "value"
    },
    exportAs: ["mdbInput"],
    standalone: false,
    features: [ɵɵProvidersFeature([{
      provide: MdbAbstractFormControl,
      useExisting: _MdbInputDirective
    }])]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MdbInputDirective, [{
    type: Directive,
    args: [{
      // eslint-disable-next-line @angular-eslint/directive-selector
      selector: "[mdbInput]",
      exportAs: "mdbInput",
      providers: [{
        provide: MdbAbstractFormControl,
        useExisting: MdbInputDirective
      }],
      standalone: false
    }]
  }], () => [{
    type: ElementRef
  }, {
    type: Renderer2
  }, {
    type: NgControl,
    decorators: [{
      type: Optional
    }, {
      type: Self
    }]
  }, {
    type: AutofillMonitor
  }, {
    type: DestroyRef
  }], {
    disabled: [{
      type: HostBinding,
      args: ["disabled"]
    }, {
      type: Input,
      args: ["disabled"]
    }],
    readonly: [{
      type: Input,
      args: ["readonly"]
    }],
    value: [{
      type: Input
    }],
    _onFocus: [{
      type: HostListener,
      args: ["focus"]
    }],
    _onBlur: [{
      type: HostListener,
      args: ["blur"]
    }]
  });
})();
var MdbFormsModule = class _MdbFormsModule {
  static ɵfac = function MdbFormsModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MdbFormsModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _MdbFormsModule,
    declarations: [MdbFormControlComponent, MdbInputDirective, MdbLabelDirective],
    imports: [CommonModule, FormsModule],
    exports: [MdbFormControlComponent, MdbInputDirective, MdbLabelDirective]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [CommonModule, FormsModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MdbFormsModule, [{
    type: NgModule,
    args: [{
      declarations: [MdbFormControlComponent, MdbInputDirective, MdbLabelDirective],
      exports: [MdbFormControlComponent, MdbInputDirective, MdbLabelDirective],
      imports: [CommonModule, FormsModule]
    }]
  }], null, null);
})();
export {
  MdbAbstractFormControl,
  MdbFormControlComponent,
  MdbFormsModule,
  MdbInputDirective,
  MdbLabelDirective
};
/*! Bundled license information:

@angular/core/fesm2022/rxjs-interop.mjs:
  (**
   * @license Angular v19.2.14
   * (c) 2010-2025 Google LLC. https://angular.io/
   * License: MIT
   *)
*/
//# sourceMappingURL=mdb-angular-ui-kit_forms.js.map

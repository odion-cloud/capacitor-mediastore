/*!
 * @capacitor/mediastore
 * Capacitor plugin for Android MediaStore API access
 * MIT Licensed
 */
!function(){"use strict";var e,r=Object.create?function(e,r,t,n){void 0===n&&(n=t);var o=Object.getOwnPropertyDescriptor(r,t);o&&!("get"in o?!r.__esModule:o.writable||o.configurable)||(o={enumerable:!0,get:function(){return r[t]}}),Object.defineProperty(e,n,o)}:function(e,r,t,n){void 0===n&&(n=t),e[n]=r[t]},t=Object.create?function(e,r){Object.defineProperty(e,"default",{enumerable:!0,value:r})}:function(e,r){e.default=r},n=(e=function(r){return e=Object.getOwnPropertyNames||function(e){var r=[];for(var t in e)Object.prototype.hasOwnProperty.call(e,t)&&(r[r.length]=t);return r},e(r)},function(n){if(n&&n.__esModule)return n;var o={};if(null!=n)for(var i=e(n),a=0;a<i.length;a++)"default"!==i[a]&&r(o,n,i[a]);return t(o,n),o}),o=function(e,t){for(var n in e)"default"===n||Object.prototype.hasOwnProperty.call(t,n)||r(t,e,n)};Object.defineProperty(exports,"__esModule",{value:!0}),exports.CapacitorMediaStore=void 0;var i=(0,require("@capacitor/core").registerPlugin)("CapacitorMediaStore",{web:function(){return Promise.resolve().then(function(){return n(require("./web"))}).then(function(e){return new e.CapacitorMediaStoreWeb})}});exports.CapacitorMediaStore=i,o(require("./definitions"),exports)}();
//# sourceMappingURL=plugin.js.map

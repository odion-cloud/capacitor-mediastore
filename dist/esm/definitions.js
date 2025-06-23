"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PermissionState = exports.MediaType = void 0;
var MediaType;
(function (MediaType) {
    MediaType["IMAGE"] = "image";
    MediaType["AUDIO"] = "audio";
    MediaType["VIDEO"] = "video";
    MediaType["DOCUMENT"] = "document";
    MediaType["ALL"] = "all";
})(MediaType || (exports.MediaType = MediaType = {}));
var PermissionState;
(function (PermissionState) {
    PermissionState["GRANTED"] = "granted";
    PermissionState["DENIED"] = "denied";
    PermissionState["PROMPT"] = "prompt";
    PermissionState["PROMPT_WITH_RATIONALE"] = "prompt-with-rationale";
})(PermissionState || (exports.PermissionState = PermissionState = {}));
//# sourceMappingURL=definitions.js.map
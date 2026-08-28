"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        if (typeof b !== "function" && b !== null)
            throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g = Object.create((typeof Iterator === "function" ? Iterator : Object).prototype);
    return g.next = verb(0), g["throw"] = verb(1), g["return"] = verb(2), typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (g && (g = 0, op[0] && (_ = 0)), _) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.CapacitorMediaStoreWeb = void 0;
var core_1 = require("@capacitor/core");
var definitions_1 = require("./definitions");
var CapacitorMediaStoreWeb = /** @class */ (function (_super) {
    __extends(CapacitorMediaStoreWeb, _super);
    function CapacitorMediaStoreWeb() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    CapacitorMediaStoreWeb.prototype.getMedias = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.getMedias() is not supported on web platform');
                throw this.unimplemented('Not implemented on web.');
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.getMediasByType = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.getMediasByType() is not supported on web platform');
                throw this.unimplemented('Not implemented on web.');
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.getAlbums = function () {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.getAlbums() is not supported on web platform');
                throw this.unimplemented('Not implemented on web.');
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.saveMedia = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.saveMedia() is not supported on web platform');
                throw this.unimplemented('Not implemented on web.');
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.getMediaMetadata = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.getMediaMetadata() is not supported on web platform');
                throw this.unimplemented('Not implemented on web.');
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.checkPermissions = function () {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.checkPermissions() is not supported on web platform');
                return [2 /*return*/, {
                        readExternalStorage: definitions_1.PermissionState.GRANTED,
                        readMediaImages: definitions_1.PermissionState.GRANTED,
                        readMediaAudio: definitions_1.PermissionState.GRANTED,
                        readMediaVideo: definitions_1.PermissionState.GRANTED,
                        writeExternalStorage: definitions_1.PermissionState.GRANTED
                    }];
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.requestPermissions = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.requestPermissions() is not supported on web platform');
                return [2 /*return*/, {
                        readExternalStorage: definitions_1.PermissionState.GRANTED,
                        readMediaImages: definitions_1.PermissionState.GRANTED,
                        readMediaAudio: definitions_1.PermissionState.GRANTED,
                        readMediaVideo: definitions_1.PermissionState.GRANTED,
                        writeExternalStorage: definitions_1.PermissionState.GRANTED
                    }];
            });
        });
    };
    // ──────────────────────────────────────────────
    // File Change Watcher (web stubs)
    // ──────────────────────────────────────────────
    CapacitorMediaStoreWeb.prototype.startWatching = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.startWatching() is not supported on web platform');
                throw this.unimplemented('Not implemented on web.');
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.stopWatching = function () {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.stopWatching() is not supported on web platform');
                throw this.unimplemented('Not implemented on web.');
            });
        });
    };
    // ──────────────────────────────────────────────
    // Native Media Session (web fallback using Web MediaSession API)
    // ──────────────────────────────────────────────
    CapacitorMediaStoreWeb.prototype.setMediaSession = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            var artwork;
            return __generator(this, function (_a) {
                if ('mediaSession' in navigator) {
                    artwork = [];
                    if (options.artworkUrl) {
                        artwork.push({ src: options.artworkUrl, sizes: '512x512', type: 'image/png' });
                    }
                    navigator.mediaSession.metadata = new MediaMetadata({
                        title: options.title,
                        artist: options.artist,
                        album: options.album || '',
                        artwork: artwork,
                    });
                }
                else {
                    console.warn('CapacitorMediaStore.setMediaSession() - Web MediaSession API not available');
                }
                return [2 /*return*/];
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.updatePlaybackState = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                if ('mediaSession' in navigator) {
                    navigator.mediaSession.playbackState = options.state === 'playing' ? 'playing' : options.state === 'paused' ? 'paused' : 'none';
                }
                return [2 /*return*/];
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.updatePositionState = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                if ('mediaSession' in navigator && navigator.mediaSession.setPositionState) {
                    try {
                        navigator.mediaSession.setPositionState({
                            duration: options.duration,
                            playbackRate: options.playbackRate || 1.0,
                            position: options.position,
                        });
                    }
                    catch (e) {
                        console.warn('CapacitorMediaStore.updatePositionState() error:', e);
                    }
                }
                return [2 /*return*/];
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.updateMediaSessionButtons = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.updateMediaSessionButtons() - Custom buttons not supported on web');
                return [2 /*return*/];
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.destroyMediaSession = function () {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                if ('mediaSession' in navigator) {
                    navigator.mediaSession.metadata = null;
                    navigator.mediaSession.playbackState = 'none';
                }
                return [2 /*return*/];
            });
        });
    };
    // ----------------------------------------------------------------
    // File Picker � Save As & Open (web stubs � web uses showSaveFilePicker directly)
    // ----------------------------------------------------------------
    CapacitorMediaStoreWeb.prototype.saveFileAs = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.saveFileAs() is not needed on web � use showSaveFilePicker directly');
                throw this.unimplemented('Not implemented on web. Use window.showSaveFilePicker instead.');
            });
        });
    };
    CapacitorMediaStoreWeb.prototype.openFile = function (options) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                console.warn('CapacitorMediaStore.openFile() is not needed on web � use <input type=file> directly');
                throw this.unimplemented('Not implemented on web. Use file input or showOpenFilePicker instead.');
            });
        });
    };
    return CapacitorMediaStoreWeb;
}(core_1.WebPlugin));
exports.CapacitorMediaStoreWeb = CapacitorMediaStoreWeb;
//# sourceMappingURL=web.js.map
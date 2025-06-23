import { nodeResolve } from '@rollup/plugin-node-resolve';
import terser from '@rollup/plugin-terser';

const banner = `
/*!
 * @capacitor/mediastore
 * Capacitor plugin for Android MediaStore API access
 * MIT Licensed
 */
`.trim();

export default {
  input: 'dist/esm/index.js',
  output: [
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorMediaStore',
      globals: {
        '@capacitor/core': 'capacitorExports',
      },
      sourcemap: true,
      banner,
    },
    {
      file: 'dist/plugin.cjs.js',
      format: 'cjs',
      sourcemap: true,
      banner,
    },
  ],
  external: ['@capacitor/core'],
  plugins: [
    nodeResolve({
      browser: true,
    }),
    terser({
      format: {
        comments: function (node, comment) {
          return comment.type === 'comment2' && /^!/.test(comment.value);
        },
      },
    }),
  ],
};

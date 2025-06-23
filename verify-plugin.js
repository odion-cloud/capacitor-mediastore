#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('🔍 Verifying Capacitor MediaStore Plugin...\n');

// Check required files exist
const requiredFiles = [
  'dist/esm/index.js',
  'dist/esm/index.d.ts',
  'dist/plugin.js',
  'dist/plugin.cjs.js',
  'android/src/main/java/com/capacitor/mediastore/MediaStorePlugin.kt',
  'android/src/main/java/com/capacitor/mediastore/MediaStoreHelper.kt',
  'ios/Plugin/CapacitorMediaStorePlugin.swift',
  'src/definitions.ts',
  'src/index.ts',
  'src/web.ts',
  'CapacitorMediaStore.podspec',
  'capacitor.config.json'
];

let allFilesExist = true;

console.log('📁 Checking required files:');
requiredFiles.forEach(file => {
  const exists = fs.existsSync(file);
  console.log(`  ${exists ? '✅' : '❌'} ${file}`);
  if (!exists) allFilesExist = false;
});

// Check TypeScript definitions
console.log('\n📝 Checking TypeScript definitions:');
try {
  const defsContent = fs.readFileSync('dist/esm/definitions.d.ts', 'utf8');
  const hasMediaStorePlugin = defsContent.includes('CapacitorMediaStorePlugin');
  const hasMediaFile = defsContent.includes('MediaFile');
  const hasPermissionState = defsContent.includes('PermissionState');
  
  console.log(`  ${hasMediaStorePlugin ? '✅' : '❌'} CapacitorMediaStorePlugin interface`);
  console.log(`  ${hasMediaFile ? '✅' : '❌'} MediaFile interface`);
  console.log(`  ${hasPermissionState ? '✅' : '❌'} PermissionState enum`);
} catch (error) {
  console.log('  ❌ Error reading TypeScript definitions');
  allFilesExist = false;
}

// Check Android implementation
console.log('\n🤖 Checking Android implementation:');
try {
  const kotlinContent = fs.readFileSync('android/src/main/java/com/capacitor/mediastore/MediaStorePlugin.kt', 'utf8');
  const hasGetMedias = kotlinContent.includes('fun getMedias');
  const hasGetAlbums = kotlinContent.includes('fun getAlbums');
  const hasPermissions = kotlinContent.includes('requestPermissions');
  const hasMediaStoreHelper = kotlinContent.includes('MediaStoreHelper');
  
  console.log(`  ${hasGetMedias ? '✅' : '❌'} getMedias method`);
  console.log(`  ${hasGetAlbums ? '✅' : '❌'} getAlbums method`);
  console.log(`  ${hasPermissions ? '✅' : '❌'} Permission handling`);
  console.log(`  ${hasMediaStoreHelper ? '✅' : '❌'} MediaStoreHelper integration`);
} catch (error) {
  console.log('  ❌ Error reading Android implementation');
  allFilesExist = false;
}

// Check plugin configuration
console.log('\n⚙️  Checking plugin configuration:');
try {
  const configContent = fs.readFileSync('capacitor.config.json', 'utf8');
  const config = JSON.parse(configContent);
  const hasPlugin = config.plugins && config.plugins.CapacitorMediaStore;
  const hasPackage = hasPlugin && config.plugins.CapacitorMediaStore.androidPackage;
  
  console.log(`  ${hasPlugin ? '✅' : '❌'} Plugin configuration`);
  console.log(`  ${hasPackage ? '✅' : '❌'} Android package mapping`);
} catch (error) {
  console.log('  ❌ Error reading plugin configuration');
  allFilesExist = false;
}

// Check built files
console.log('\n🔨 Checking built files:');
try {
  const esmIndex = fs.readFileSync('dist/esm/index.js', 'utf8');
  const pluginJs = fs.readFileSync('dist/plugin.js', 'utf8');
  
  const hasRegisterPlugin = esmIndex.includes('registerPlugin');
  const hasIIFE = pluginJs.includes('capacitorMediaStore');
  
  console.log(`  ${hasRegisterPlugin ? '✅' : '❌'} Plugin registration`);
  console.log(`  ${hasIIFE ? '✅' : '❌'} IIFE bundle`);
} catch (error) {
  console.log('  ❌ Error reading built files');
  allFilesExist = false;
}

// Final verification
console.log('\n🎯 Final Verification:');
if (allFilesExist) {
  console.log('✅ All checks passed! Plugin is ready for use.');
  console.log('\n📦 To use this plugin in your Capacitor project:');
  console.log('1. Copy this plugin directory to your project');
  console.log('2. Run: npm install ./path-to-plugin');
  console.log('3. Run: npx cap sync android');
  console.log('4. Add required permissions to AndroidManifest.xml');
  console.log('5. Import and use: import { CapacitorMediaStore } from "@capacitor/mediastore"');
} else {
  console.log('❌ Some checks failed. Please review the errors above.');
  process.exit(1);
}
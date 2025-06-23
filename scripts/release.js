#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');

const releaseType = process.argv[2] || 'patch';

if (!['patch', 'minor', 'major'].includes(releaseType)) {
  console.error('Usage: node scripts/release.js [patch|minor|major]');
  process.exit(1);
}

try {
  console.log(`🚀 Creating ${releaseType} release...`);
  
  // Update version
  execSync(`npm version ${releaseType}`, { stdio: 'inherit' });
  
  // Get new version
  const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  const newVersion = packageJson.version;
  
  // Push changes and tags
  execSync('git push origin main --tags', { stdio: 'inherit' });
  
  console.log(`✅ Version ${newVersion} released!`);
  console.log(`📦 Create GitHub release at: https://github.com/odion-cloud/capacitor-mediastore/releases/new?tag=v${newVersion}`);
  
} catch (error) {
  console.error('❌ Release failed:', error.message);
  process.exit(1);
} 
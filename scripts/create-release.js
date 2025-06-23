#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');

const releaseType = process.argv[2] || 'patch';

if (!['patch', 'minor', 'major'].includes(releaseType)) {
  console.error('Usage: node scripts/create-release.js [patch|minor|major]');
  process.exit(1);
}

try {
  console.log(`🚀 Creating ${releaseType} release...`);
  
  // Update version
  execSync(`npm version ${releaseType}`, { stdio: 'inherit' });
  
  // Get new version
  const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  const newVersion = packageJson.version;
  const tagName = `v${newVersion}`;
  
  // Push changes and tags
  execSync('git push origin main --tags', { stdio: 'inherit' });
  
  console.log(`\n✅ Version ${newVersion} tagged and pushed!`);
  console.log(`\n📦 NEXT STEP: Create GitHub Release to trigger automatic npm publishing:`);
  console.log(`🔗 ${generateReleaseUrl(tagName, releaseType, newVersion)}`);
  console.log(`\n💡 After creating the release, GitHub Actions will automatically:`);
  console.log(`   ✅ Build the package`);
  console.log(`   ✅ Publish to npm registry`);
  console.log(`   ✅ Publish to GitHub Packages`);
  
} catch (error) {
  console.error('❌ Release failed:', error.message);
  process.exit(1);
}

function generateReleaseUrl(tagName, releaseType, newVersion) {
  const baseUrl = 'https://github.com/odion-cloud/capacitor-mediastore/releases/new';
  const title = `${tagName} - ${releaseType.charAt(0).toUpperCase() + releaseType.slice(1)} Update`;
  const body = `## What's Changed in ${tagName}

### ${releaseType.charAt(0).toUpperCase() + releaseType.slice(1)} Update

- Package version updated to ${newVersion}
- Automated publishing to npm and GitHub Packages

**Installation:**
\`\`\`bash
npm install @odion-cloud/capacitor-mediastore@${newVersion}
npx cap sync
\`\`\`

**Full Changelog:** https://github.com/odion-cloud/capacitor-mediastore/compare/v${getPreviousVersion(newVersion)}...${tagName}`;

  return `${baseUrl}?tag=${tagName}&title=${encodeURIComponent(title)}&body=${encodeURIComponent(body)}`;
}

function getPreviousVersion(currentVersion) {
  const [major, minor, patch] = currentVersion.split('.').map(Number);
  if (patch > 0) return `${major}.${minor}.${patch - 1}`;
  if (minor > 0) return `${major}.${minor - 1}.0`;
  if (major > 0) return `${major - 1}.0.0`;
  return '0.0.0';
} 
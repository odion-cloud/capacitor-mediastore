#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');

const releaseType = process.argv[2] || 'patch';

if (!['patch', 'minor', 'major'].includes(releaseType)) {
  console.error('Usage: node scripts/create-release.js [patch|minor|major]');
  process.exit(1);
}

try {
  console.log(`🚀 Creating ${releaseType} release with GitHub release...`);
  
  // Update version
  execSync(`npm version ${releaseType}`, { stdio: 'inherit' });
  
  // Get new version
  const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  const newVersion = packageJson.version;
  const tagName = `v${newVersion}`;
  
  // Push changes and tags
  execSync('git push origin main --tags', { stdio: 'inherit' });
  
  // Create GitHub release (requires GitHub CLI)
  const releaseNotes = `## What's Changed in ${tagName}

### ${releaseType.charAt(0).toUpperCase() + releaseType.slice(1)} Update

- Package version updated to ${newVersion}
- Automated publishing to npm and GitHub Packages
- See full changelog at: https://github.com/odion-cloud/capacitor-mediastore/compare/v${getPreviousVersion(newVersion)}...${tagName}

**Installation:**
\`\`\`bash
npm install @odion-cloud/capacitor-mediastore@${newVersion}
npx cap sync
\`\`\``;

  // Create GitHub release using gh CLI
  try {
    execSync(`gh release create ${tagName} --title "${tagName} - ${releaseType.charAt(0).toUpperCase() + releaseType.slice(1)} Update" --notes "${releaseNotes}"`, { stdio: 'inherit' });
    console.log(`✅ GitHub release ${tagName} created! 🎉`);
    console.log(`📦 Publishing to npm will happen automatically via GitHub Actions`);
    console.log(`🔗 View release: https://github.com/odion-cloud/capacitor-mediastore/releases/tag/${tagName}`);
  } catch (error) {
    console.log(`⚠️  GitHub release creation failed. Create it manually at:`);
    console.log(`🔗 https://github.com/odion-cloud/capacitor-mediastore/releases/new?tag=${tagName}`);
  }
  
} catch (error) {
  console.error('❌ Release failed:', error.message);
  process.exit(1);
}

function getPreviousVersion(currentVersion) {
  const [major, minor, patch] = currentVersion.split('.').map(Number);
  if (patch > 0) return `${major}.${minor}.${patch - 1}`;
  if (minor > 0) return `${major}.${minor - 1}.0`;
  if (major > 0) return `${major - 1}.0.0`;
  return '0.0.0';
} 
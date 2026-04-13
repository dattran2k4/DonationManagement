const { defineConfig } = require('cypress');

module.exports = defineConfig({
  projectId: '7qpoc3',
  e2e: {
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: false,
    video: false,
    screenshotOnRunFailure: true,
    viewportWidth: 1440,
    viewportHeight: 1080,
    defaultCommandTimeout: 10000
  }
});

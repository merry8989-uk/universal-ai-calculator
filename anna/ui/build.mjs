#!/usr/bin/env node
/*
 * Builds the UI bundle from the one copy of the page the repository already has.
 *
 * The Android app loads app/src/main/assets/formula-studio.html; the public site
 * serves the same bytes as docs/formulas.html. Anna gets a third copy, and the
 * way to keep three copies honest is to generate this one rather than edit it.
 */
import { copyFileSync, mkdirSync, statSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const source = join(here, '..', '..', 'app', 'src', 'main', 'assets', 'formula-studio.html');
const dist = join(here, 'dist');
mkdirSync(dist, { recursive: true });
copyFileSync(source, join(dist, 'index.html'));
const kb = Math.round(statSync(join(dist, 'index.html')).size / 1024);
console.log(`ui/dist/index.html written from the app asset (${kb} KB)`);

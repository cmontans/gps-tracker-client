// sanitize.js - Input sanitization and output escaping helpers.
// Extracted into its own module so the security-critical logic is unit-testable.

// Input limits (mirror client-side maxlength attributes)
const MAX_USERNAME_LEN = 20;
const MAX_GROUPNAME_LEN = 30;
const MAX_USERID_LEN = 64;

// Trim + cap a free-text field; strip control chars that could break logs/output.
function sanitizeText(value, maxLen) {
  if (typeof value !== 'string') return '';
  let out = '';
  for (const ch of value) {
    const code = ch.codePointAt(0);
    // Drop C0/C1 control characters (0x00-0x1F, 0x7F-0x9F)
    if (code <= 0x1F || (code >= 0x7F && code <= 0x9F)) continue;
    out += ch;
  }
  return out.trim().slice(0, maxLen);
}

// Normalize a group name consistently everywhere (WS, REST, KML): trim, cap, lowercase.
function normalizeGroupName(value) {
  const cleaned = sanitizeText(value, MAX_GROUPNAME_LEN);
  return (cleaned || 'default').toLowerCase();
}

// Escape a value for safe inclusion in XML/KML (also neutralizes CDATA breakout
// via ]]> and HTML injection, since Google Earth renders the CDATA as HTML).
function escapeXml(value) {
  return String(value == null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

module.exports = {
  MAX_USERNAME_LEN,
  MAX_GROUPNAME_LEN,
  MAX_USERID_LEN,
  sanitizeText,
  normalizeGroupName,
  escapeXml
};

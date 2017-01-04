// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This is the shared code for the new (Chrome 37) security interstitials. It is
// used for both SSL interstitials and Safe Browsing interstitials.

var expandedDetails = false;
var keyPressState = 0;

// Should match SecurityInterstitialCommands in security_interstitial_page.h
var CMD_DONT_PROCEED = 0;
var CMD_PROCEED = 1;
var CMD_SHOW_MORE_SECTION = 2;

/**
 * A convenience method for sending commands to the parent page.
 * @param {string} cmd  The command to send.
 */
function sendCommand(cmd) {
<if expr="not is_ios">
  window.domAutomationController.setAutomationId(1);
  window.domAutomationController.send(cmd);
</if>
<if expr="is_ios">
  // TODO(crbug.com/565877): Revisit message passing for WKWebView.
  var iframe = document.createElement('IFRAME');
  iframe.setAttribute('src', 'js-command:' + cmd);
  document.documentElement.appendChild(iframe);
  iframe.parentNode.removeChild(iframe);
</if>
}

function setupEvents() {

  $('primary-button').addEventListener('click', function() {
          sendCommand(CMD_DONT_PROCEED);
  });

  $('proceed-link').addEventListener('click', function(event) {
    sendCommand(CMD_PROCEED);
  });

  $('info-link').addEventListener('click', function(event) {
    sendCommand(CMD_SHOW_MORE_SECTION);
  });

  $('details-button').classList.add('hidden');

  preventDefaultOnPoundLinkClicks();
}

document.addEventListener('DOMContentLoaded', setupEvents);

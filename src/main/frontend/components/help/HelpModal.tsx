/**
 * HelpModal - Native HTML dialog with glassmorphism styling.
 *
 * Uses native <dialog> element for built-in:
 * - Focus trapping
 * - Escape key handling
 * - Backdrop with ::backdrop pseudo-element
 * - Proper inert state for background content
 */

import React, { useEffect, useRef, useCallback } from 'react';
import { X } from 'lucide-react';
import type { HelpModalProps, HelpSectionId } from './types';
import { getRegisteredHelp } from './HelpRegistry';
import { HelpNavigation } from './HelpNavigation';
import { HelpSection } from './HelpSection';
import { AboutSection } from './AboutSection';
import { useScrollspy } from './useScrollspy';
import './HelpModal.css';

// Import help content to trigger registration
import '../cards/propagation/PropagationHelp';
import '../cards/activations/ActivationsHelp';
import '../cards/contests/ContestsHelp';
import '../cards/meteor-showers/MeteorShowersHelp';

export function HelpModal({ isOpen, onClose }: HelpModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const sections = getRegisteredHelp();

  const { activeSectionId, containerRef, scrollToSection } = useScrollspy(sections);

  // Sync dialog state with isOpen prop
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (isOpen && !dialog.open) {
      dialog.showModal();
    } else if (!isOpen && dialog.open) {
      dialog.close();
    }
  }, [isOpen]);

  // Handle native dialog close events (Escape, form submission)
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    const handleClose = () => onClose();
    dialog.addEventListener('close', handleClose);

    return () => dialog.removeEventListener('close', handleClose);
  }, [onClose]);

  // Handle backdrop click (click on dialog but outside content)
  const handleBackdropClick = useCallback(
    (e: React.MouseEvent<HTMLDialogElement>) => {
      if (e.target === dialogRef.current) {
        onClose();
      }
    },
    [onClose],
  );

  const handleNavigate = useCallback(
    (sectionId: HelpSectionId) => {
      scrollToSection(sectionId);
    },
    [scrollToSection],
  );

  return (
    <dialog
      ref={dialogRef}
      className="help-modal"
      onClick={handleBackdropClick}
      aria-labelledby="help-modal-title"
    >
      <div className="help-modal__container">
        <header className="help-modal__header">
          <h2 id="help-modal-title" className="help-modal__title">
            Help & About
          </h2>
          <button
            type="button"
            className="help-modal__close"
            onClick={onClose}
            aria-label="Close help"
          >
            <X size={20} aria-hidden="true" />
          </button>
        </header>

        <HelpNavigation
          sections={sections}
          activeSectionId={activeSectionId}
          onNavigate={handleNavigate}
        />

        <div ref={containerRef} className="help-modal__content">
          <AboutSection />
          {sections.map((section) => (
            <HelpSection key={section.id} definition={section}>
              <section.Content />
            </HelpSection>
          ))}
        </div>
      </div>
    </dialog>
  );
}

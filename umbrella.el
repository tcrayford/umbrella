(defun foo ()
  (interactive)
  (message (slime-current-package)))

;; TODO
;; Use slime-symbol-at-point somewhere

(require 'clojure-mode)
(require 'cl)
(require 'slime)
(require 'swank-clojure)
(require 'which-func)


;; Faces

 (defface clojure-umbrella-bad-duplication-face
   '((((class color) (background light))
      :background "orange red") ;; TODO: Hard to read strings over this.
     (((class color) (background dark))
      :background "firebrick"))
   "Face for showing duplicated code"
   :group 'clojure-umbrella-mode)

;; (setq clojure-umbrella-bad-duplication-face
;;       '((((class color) (background light))
;;          :background "orange red") ;; TODO: Hard to read strings over this.
;;         (((class color) (background dark))
;;          :background "firebrick")))

;; Support functions

(defun clojure-umbrella-eval (string &optional handler)
  (slime-eval-async `(swank:eval-and-grab-output ,string)
                    (or handler #'identity)))

(defun clojure-umbrella-eval-sync (string)
  (slime-eval `(swank:eval-and-grab-output ,string)))

(defun clojure-umbrella-get-results ()
  (progn
    (clojure-umbrella-eval-sync (format "(require '%s :reload)"
                                        (slime-current-package)))
   (read
    (car
     (cdr
      (clojure-umbrella-eval-sync
       (concat "(require 'umbrella.core) (in-ns 'umbrella.core)
    (umbrella-run '" (slime-current-package) ")")))))))

(defun clojure-umbrella-message-results ()
  (interactive)
  (message "%s" (clojure-umbrella-get-results)))

(defun clojure-umbrella-run ()
  (interactive)
  (clojure-umbrella-clear)
  (clojure-umbrella-highlight-problems
   (clojure-umbrella-get-results)))

(defun clojure-umbrella-highlight-problems (swank-output)
  (dolist (problem swank-output)
    (apply #'clojure-umbrella-highlight-problem
           (mapcar (lambda (x) (cadr x)) problem)))
  (message (format "%s" swank-output)))

(defun clojure-umbrella-highlight-problem (line text message)
  (save-excursion
    (goto-line line)
    (progn
      (search-forward text nil t)
      (let ((beg (match-beginning 0))
            (end (match-end 0)))
        (let ((overlay (make-overlay beg end)))
          (overlay-put overlay 'face clojure-umbrella-bad-duplication-face)
          (overlay-put overlay 'message message))))))

(defun clojure-umbrella-clear ()
  "Remove overlays and clear stored results."
  (interactive)
  (remove-overlays))

(defun clojure-umbrella-show-result ()
  "Show the result of the test under point."
  (interactive)
  (let ((overlay (find-if (lambda (o) (overlay-get o 'message))
                          (overlays-at (point)))))
    (if overlay
        (message (replace-regexp-in-string "%" "%%"
                                           (overlay-get overlay 'message))))))

(defvar clojure-umbrella-mode-map
  (let ((map (make-sparse-keymap)))
    (define-key map (kbd "C-c C-/") 'clojure-umbrella-run)
    (define-key map (kbd "C-c C-u") 'clojure-umbrella-show-result)
    map)
  "Keymap for Clojure umbrella mode.")

;;;###autoload
(define-minor-mode clojure-umbrella-mode
  "A minor mode for keeping Clojure code DRY"
  (when (slime-connected-p)
    (run-hooks 'slime-connected-hook)))

(progn
  (defun clojure-umbrella-enable ()
    (clojure-umbrella-mode t))
  (add-hook 'clojure-mode-hook 'clojure-umbrella-enable))

(provide 'clojure-umbrella-mode)

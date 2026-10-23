/** A message box for something that went wrong, with an optional retry button. */
export default function ErrorBox({ message, onRetry }) {
  return (
    <div className="error-box" role="alert">
      <p>{message}</p>
      {onRetry && <button type="button" className="button button--outline" onClick={onRetry}>Try again</button>}
    </div>
  );
}

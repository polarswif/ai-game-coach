/** A spinning ring plus what's happening. role="status" makes screen readers announce it. */
export default function Spinner({ label }) {
  return (
    <div className="spinner" role="status">
      <span className="spinner__ring" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

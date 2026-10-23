import { useEffect, useId, useState } from 'react';

/**
 * A box you can click to pick images, or drop images onto. Shows a preview of what was chosen.
 * `files` is the chosen File objects (from the parent's state); `onFiles` replaces them.
 */
export default function DropZone({ title, badge, badgeKind, description, files, onFiles, multiple = false, soft = false }) {
  const inputId = useId(); // a unique id, so the <label> is tied to its own <input>
  const [dragging, setDragging] = useState(false);
  const [previews, setPreviews] = useState([]);

  // Build preview URLs for the chosen images, and release them when the files change or the
  // component goes away (the cleanup function), so the browser doesn't hold on to the memory.
  useEffect(() => {
    const urls = files.map((file) => URL.createObjectURL(file));
    setPreviews(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [files]);

  function accept(fileList) {
    const images = Array.from(fileList).filter((file) => file.type.startsWith('image/'));
    if (images.length > 0) {
      onFiles(multiple ? images : images.slice(0, 1));
    }
  }

  const classes = ['drop-zone', soft ? 'drop-zone--soft' : '', dragging ? 'drop-zone--dragging' : '', files.length ? 'drop-zone--filled' : '']
    .filter(Boolean).join(' ');

  return (
    <label htmlFor={inputId} className={classes}
      onDragOver={(event) => { event.preventDefault(); setDragging(true); }}
      onDragLeave={() => setDragging(false)}
      onDrop={(event) => { event.preventDefault(); setDragging(false); accept(event.dataTransfer.files); }}>
      <span className="drop-zone__head">
        <span className="drop-zone__title">{title}</span>
        {badge && <span className={`badge badge--${badgeKind}`}>{badge}</span>}
      </span>
      <span className="drop-zone__description">{description}</span>
      {files.length > 0 ? (
        <span className="drop-zone__previews">
          {previews.map((url, index) => (
            <img key={url} src={url} alt={`Chosen screenshot ${index + 1}: ${files[index]?.name ?? ''}`} />
          ))}
        </span>
      ) : null}
      <span className="drop-zone__action">
        {files.length > 0 ? `${files.map((f) => f.name).join(', ')} · change` : multiple ? 'Drop images or browse' : 'Drop image or browse'}
      </span>
      <input id={inputId} type="file" className="visually-hidden" accept="image/png,image/jpeg,image/webp"
        multiple={multiple} onChange={(event) => accept(event.target.files)} />
    </label>
  );
}
